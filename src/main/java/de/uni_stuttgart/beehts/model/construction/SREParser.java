package de.uni_stuttgart.beehts.model.construction;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;

import de.uni_stuttgart.beehts.model.SRE;
import de.uni_stuttgart.beehts.model.Tuple;

/**
 * This class helps creating SREs from Strings.
 * 
 * @author Tobias Beeh
 */
class SREParser {

	private static class Token {

		public enum Type {
			DELIM_SUM, DELIM_CAT, DELIM_KLEENE, PAREN_OPEN, PAREN_CLOSE, BRACKET_OPEN, BRACKET_CLOSE, IDENTIFIER, RATE,
		}

		public final Type type;
		public final String content;

		public Token(Type type, String content) {
			this.type = type;
			this.content = content;
		}

		public Token(Type type) {
			this(type, "");
		}

		@Override
		public String toString() {
			switch (type) {
			case DELIM_SUM:
				return "+";
			case DELIM_CAT:
				return ":";
			case DELIM_KLEENE:
				return "*";
			case PAREN_OPEN:
				return "(";
			case PAREN_CLOSE:
				return ")";
			case BRACKET_OPEN:
				return "[";
			case BRACKET_CLOSE:
				return "]";
			default:
				return content;
			}
		}
	}

	/**
	 * Parse an SRE from a String.
	 * 
	 * @param s
	 *            the String to parse.
	 * @return A constructed SRE.
	 */
	public static SRE parse(String s) {
		return buildSRE(tokenize(s));
	}

	private static List<Token> tokenize(String s) {
		List<String> tokens = splitAtTokens(s);
		tokens.replaceAll(x -> x.trim());
		List<Token> list = new ArrayList<>(tokens.size());
		tokens.stream().filter(x -> !x.isEmpty()).forEach(x -> list.add(stringToToken(x)));
		return list;
	}

	private static SRE buildSRE(List<Token> tokens) {
		return buildSRE(tokens, 0, tokens.size() - 1);
	}

	/**
	 * Build an SRE from a list of tokens.
	 * 
	 * @param tokens
	 *            The tokens recognized from the String.
	 * @param firstIdx
	 *            The index of the first token to include.
	 * @param lastIdx
	 *            The index of the last token to include.
	 * @return An SRE representing the tokens.
	 */
	private static SRE buildSRE(List<Token> tokens, int firstIdx, int lastIdx) {
		List<Tuple<SRE, Integer>> sres = new LinkedList<>();
		boolean lookingForDelimiter = false;
		Token.Type outerDelimType = null;
		for (int i = firstIdx; i <= lastIdx; i++) {
			switch (tokens.get(i).type) {
			case PAREN_OPEN: {
				if (lookingForDelimiter) {
					throw new InputMismatchException("Was looking for a delimiter, got a '('");
				}
				int startIdx = i;
				int nestedParenthesis = 0;
				searchClosing: while (true) {
					i++;
					if (i > lastIdx)
						throw new InputMismatchException("No matching closing parenthesis found.");
					switch (tokens.get(i).type) {
					case PAREN_OPEN:
						nestedParenthesis++;
						break;
					case PAREN_CLOSE: {
						if (nestedParenthesis > 0) {
							nestedParenthesis--;
						} else {
							break searchClosing;
						}
						break;
					}
					default:
						break;
					}
				}
				int rate = getRate(tokens, i + 1);
				sres.add(new Tuple<>(buildSRE(tokens, startIdx + 1, i - 1), rate));
				if (rate >= 0)
					i += 3;
				lookingForDelimiter = true;
				break;
			}
			case IDENTIFIER: {
				if (lookingForDelimiter) {
					throw new InputMismatchException(
							"Was looking for a delimiter, got an identifier: " + tokens.get(i).content);
				}
				int rate = getRate(tokens, i + 1);
				sres.add(new Tuple<>(SREBuilder.atomic(tokens.get(i).content), rate));
				if (rate >= 0)
					i += 3;
				lookingForDelimiter = true;
				break;
			}
			case DELIM_SUM: {
				if (!lookingForDelimiter) {
					throw new InputMismatchException("Was looking for an Identifier, got a sum sign");
				}
				if (outerDelimType != null && outerDelimType != Token.Type.DELIM_SUM) {
					throw new InputMismatchException("Please use parenthesis to clarify your intention.");
				} else {
					outerDelimType = Token.Type.DELIM_SUM;
				}
				lookingForDelimiter = false;
				break;
			}
			case DELIM_CAT: {
				if (!lookingForDelimiter) {
					throw new InputMismatchException("Was looking for an Identifier, got a concatenation");
				}
				if (outerDelimType != null && outerDelimType != Token.Type.DELIM_CAT) {
					throw new InputMismatchException("Please use parenthesis to clarify your intention.");
				} else {
					outerDelimType = Token.Type.DELIM_CAT;
				}
				lookingForDelimiter = false;
				break;
			}
			case DELIM_KLEENE: {
				boolean isRateInBrackets = tokens.get(i + 1).type == Token.Type.BRACKET_OPEN
						&& tokens.get(i + 3).type == Token.Type.BRACKET_CLOSE;
				int ratePosition = i + 1 + (isRateInBrackets ? 1 : 0);
				Tuple<SRE, Integer> sre = sres.remove(sres.size() - 1);
				if (!lookingForDelimiter)
					throw new InputMismatchException("Was looking for an Identifier, got a kleene star");
				else if (sre.y >= 0)
					throw new InputMismatchException("Illegal input: I do not know what 'identifier[rate]*' means.");
				else if (tokens.get(ratePosition).type != Token.Type.RATE)
					throw new InputMismatchException("You need to specify a rate for the kleene star");
				double rate = Double.parseDouble(tokens.get(ratePosition).content);
				if (rate < 0 || rate > 1)
					throw new InputMismatchException("Please specify a rate between 0 and 1 for the kleene iteration");
				int sumRate = getRate(tokens, i + 2);
				sres.add(new Tuple<>(SREBuilder.kleene(sre.x, rate), sumRate));
				if (sumRate >= 0)
					i += 4;
				else
					i++;
				if (isRateInBrackets)
					i += 2;
				break;
			}
			default:
				throw new InputMismatchException(
						"Token " + tokens.get(i).toString() + " does not fit into this context.");
			}
		}
		if (sres.size() == 0) {
			throw new InputMismatchException("ERR: Malformed or empty SRE.");
		} else if (sres.size() == 1) {
			return sres.get(0).x;
		} else {
			switch (outerDelimType) {
			case DELIM_CAT:
				sres.forEach(sre -> {
					if (sre.y >= 0)
						throw new InputMismatchException("You must not specify a rate for a concatenation!");
				});
				return SREBuilder.concat(sres.stream().map(sre -> sre.x).toArray(SRE[]::new));
			case DELIM_SUM:
				sres.forEach(x -> {
					if (x.y < 0)
						throw new InputMismatchException("You must provide a nonnegative integer rate for a sum!");
				});
				@SuppressWarnings("unchecked")
				Tuple<SRE, Integer>[] tmp = new Tuple[sres.size()];
				return SREBuilder.sum(sres.toArray(tmp));
			default:
				throw new InputMismatchException("ERR: Malformed SRE.");
			}
		}
	}

	private static int getRate(List<Token> tokens, int i) {
		if (i + 2 < tokens.size() && tokens.get(i).type == Token.Type.BRACKET_OPEN
				&& tokens.get(i + 1).type == Token.Type.RATE && tokens.get(i + 2).type == Token.Type.BRACKET_CLOSE) {
			if (tokens.get(i + 1).content.matches(".*\\..*")) {
				throw new InputMismatchException("You must not use double values for a sum rate!");
			}
			return Integer.parseInt(tokens.get(i + 1).content);
		} else {
			return -1;
		}
	}

	private static Token stringToToken(String x) {
		switch (x) {
		case "(":
			return new Token(Token.Type.PAREN_OPEN);
		case ")":
			return new Token(Token.Type.PAREN_CLOSE);
		case "[":
			return new Token(Token.Type.BRACKET_OPEN);
		case "]":
			return new Token(Token.Type.BRACKET_CLOSE);
		case "+":
			return new Token(Token.Type.DELIM_SUM);
		case ":":
			return new Token(Token.Type.DELIM_CAT);
		case "*":
			return new Token(Token.Type.DELIM_KLEENE);
		default:
			if (x.matches("\\d+(\\.\\d+)?")) {
				return new Token(Token.Type.RATE, x);
			} else if (x.matches("[A-Za-z_\\\\]\\w*")) {
				return new Token(Token.Type.IDENTIFIER, x);
			} else {
				throw new InputMismatchException("Could not parse the following string: " + x);
			}
		}
	}

	private static List<String> splitAtTokens(String s) {
		List<String> tokens = new LinkedList<>();
		int lastIndex = 0;
		for (int i = 0; i < s.length(); i++) {
			switch (s.charAt(i)) {
			case '(':
			case ')':
			case '+':
			case ':':
			case '*':
			case '[':
			case ']':
				String lastToken = s.substring(lastIndex, i);
				if (!lastToken.matches("\\s*"))
					tokens.add(lastToken);
				tokens.add(s.charAt(i) + "");
				lastIndex = i + 1;
				break;
			default:
				break;
			}
		}
		tokens.add(s.substring(lastIndex, s.length()));
		return tokens;
	}
}
