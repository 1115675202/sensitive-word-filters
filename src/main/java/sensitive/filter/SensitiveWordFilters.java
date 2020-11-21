package sensitive.filter;

import java.util.*;
import java.util.function.Predicate;

/**
 * 敏感词检查器
 *
 * @author LiuBing
 * @date 2020/11/21
 */
public class SensitiveWordFilters {

	/**
	 * 如词典中有敏感词：[敏感, 敏感词]
	 * true
	 * ├── 匹配到[敏感]结束匹配
	 * └── 比较省时，作简单判断用
	 * false
	 * ├── 匹配到[敏感词]才结束匹配
	 * └── 相对费时，但是在替换敏感词的时候，能够替换掉更多匹配数据
	 */
	private static final boolean SIMPLE_MATCH = false;

	/**
	 * 忽略字符列表
	 */
	private static final List<Character> IGNORE_CHAR_LIST = ignoreCharListInit();

	/**
	 * 忽略部分字符
	 * 如词典中有敏感词：[敏感词]，现验证文本[敏 感 词]，也会认定为敏感词，因为忽略了空格符
	 * 同样在 重构字典、往字典中加敏感词时也会使用此断言
	 */
	private static final Predicate<Character> CHAR_IGNORE =
			character -> Character.isSpaceChar(character) || IGNORE_CHAR_LIST.contains(character);

	/**
	 * 重构字典
	 */
	public static void refactoringBy(List<String> sensitiveWordList) {
		refactor(sensitiveWordList);
	}

	/**
	 * 往字典中加敏感词
	 */
	public static void add(List<String> sensitiveWordList) {
		sensitiveWordList.forEach(word -> recordToThe(SensitiveWordCache.dictionary, word));
	}

	/**
	 * 往字典中加敏感词
	 */
	public static void add(String sensitiveWord) {
		recordToThe(SensitiveWordCache.dictionary, sensitiveWord);
	}

	/**
	 * true：text 中有敏感词
	 */
	public static boolean foundIn(String text) {
		if (isEmpty(text)) {
			return false;
		}

		for (int i = 0; i < text.length(); i++) {
			if (checkSensitiveWord(text, i) > 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 从 text 中找出敏感词
	 */
	public static Set<String> findOutFrom(String text) {
		if (isEmpty(text)) {
			return Collections.emptySet();
		}

		Set<String> resultSet = new TreeSet<>((o1, o2) -> o1.length() == o2.length() ? o1.compareTo(o2) : o2.length() - o1.length());
		for (int i = 0; i < text.length(); i++) {
			int endIndex = checkSensitiveWord(text, i);
			if (endIndex > 0) {
				resultSet.add(text.substring(i, ++endIndex));
			}
		}

		return resultSet;
	}

	/**
	 * 替换 text 中的敏感词，每个字符换一个替换符
	 *
	 * @param text        文本
	 * @param replaceChar 替换符
	 * @return 替换后的文本
	 */
	public static String replace(String text, String replaceChar) {
		Set<String> sensitiveWordSet = findOutFrom(text);
		if (sensitiveWordSet.isEmpty()) {
			return text;
		}

		for (String sensitiveWord : sensitiveWordSet) {
			text = text.replace(sensitiveWord, replacementOf(replaceChar, sensitiveWord.length()));
		}
		return text;
	}

	/**
	 * 字典缓存
	 */
	private static class SensitiveWordCache {

		/**
		 * 字典/字典根节点
		 */
		static Node dictionary;

		static {
			dictionary = new Node();
			dictionary.children = new HashMap<>(16);
		}

		private SensitiveWordCache() {
		}
	}

	/**
	 * 重构字典
	 *
	 * @param sensitiveWordList 敏感字符列表
	 */
	private static void refactor(List<String> sensitiveWordList) {
		Node newDictionary = new Node();
		newDictionary.children = new HashMap<>(16);
		synchronized (SensitiveWordCache.class) {
			for (String word : sensitiveWordList) {
				recordToThe(newDictionary, word);
			}
			SensitiveWordCache.dictionary = newDictionary;
		}
	}

	/**
	 * 将敏感字符记录在节点上
	 *
	 * @param node 节点
	 * @param word 敏感字符
	 */
	private static void recordToThe(Node node, String word) {
		Objects.requireNonNull(node);
		synchronized (SensitiveWordCache.class) {
			for (int i = 0, lastIndex = word.length() - 1; i < word.length(); i++) {
				Character key = Character.valueOf(word.charAt(i));

				if (!CHAR_IGNORE.test(key)) {
					// 放置子节点
					Node next = node.get(key);
					if (Objects.isNull(next)) {
						next = new Node();
						node.putChild(key, next);
					}
					node = next;
				}

				if (i == lastIndex) {
					node.isEnd = true;
				}
			}
		}
	}

	/**
	 * 从 startIndex 开始匹配敏感字符
	 *
	 * @param text       文本
	 * @param startIndex 文本起始位置
	 * @return 0-没有敏感字符，>0 敏感字符终止位置
	 */
	private static int checkSensitiveWord(String text, int startIndex) {
		int endIndex = 0;
		Node node = SensitiveWordCache.dictionary;
		for (int i = startIndex; i < text.length(); i++) {
			Character key = Character.valueOf(text.charAt(i));

			if (CHAR_IGNORE.test(key)) {
				continue;
			}

			node = node.get(key);
			if (Objects.isNull(node)) {
				break;
			}

			if (node.isEnd) {
				endIndex = i;
				if (SIMPLE_MATCH) {
					break;
				}
			}
		}

		return endIndex;
	}

	private static boolean isEmpty(String str) {
		return str == null || "".equals(str);
	}

	/**
	 * 生成完整的替换符
	 *
	 * @param replaceChar 单字符替换符
	 * @param num         替换数量
	 * @return 完整替换符
	 */
	private static String replacementOf(String replaceChar, int num) {
		int minJointLength = 2;
		if (num < minJointLength) {
			return replaceChar;
		}

		StringBuilder replacement = new StringBuilder();
		for (int i = 0; i < num; i++) {
			replacement.append(replaceChar);
		}
		return replacement.toString();
	}

	/**
	 * 字典数据节点
	 */
	private static class Node {
		/**
		 * true：敏感词结尾
		 */
		boolean isEnd;

		/**
		 * 子节点列表
		 */
		Map<Character, Node> children;

		Node get(Character key) {
			return Objects.nonNull(children) ? children.get(key) : null;
		}

		void putChild(Character key, Node node) {
			if (Objects.isNull(children)) {
				children = new HashMap<>(16);
			}
			children.put(key, node);
		}
	}

	/**
	 * 初始化忽略字符列表
	 */
	private static List<Character> ignoreCharListInit() {
		List<Character> ignoreCharList = new ArrayList<>(10);
		ignoreCharList.add(Character.valueOf('|'));
		ignoreCharList.add(Character.valueOf('-'));
		return Collections.unmodifiableList(ignoreCharList);
	}

	private SensitiveWordFilters() {
	}
}