package GroupBased;

import java.util.Random;

public class LoremIpsumGenerator {
    // Common Lorem Ipsum words
    private static final String[] LOREM_WORDS = {
            "lorem", "ipsum", "dolor", "sit", "amet", "consectetur",
            "adipiscing", "elit", "sed", "do", "eiusmod", "tempor",
            "incididunt", "ut", "labore", "et", "dolore", "magna",
            "aliqua", "ut", "enim", "ad", "minim", "veniam", "quis",
            "nostrud", "exercitation", "ullamco", "laboris", "nisi",
            "aliquip", "ex", "ea", "commodo", "consequat", "duis",
            "aute", "irure", "dolor", "in", "reprehenderit", "in",
            "voluptate", "velit", "esse", "cillum", "dolore", "eu",
            "fugiat", "nulla", "pariatur", "excepteur", "sint",
            "occaecat", "cupidatat", "non", "proident", "sunt", "in",
            "culpa", "qui", "officia", "deserunt", "mollit", "anim",
            "id", "est", "laborum"
    };

    private static final Random random = new Random();

    public static void main(String[] args) {
        // Generate 5 examples of different lengths
        System.out.println("Short Lorem Ipsum:");
        System.out.println(generateLoremIpsum(10));

        System.out.println("\nMedium Lorem Ipsum:");
        System.out.println(generateLoremIpsum(50));

        System.out.println("\nLong Lorem Ipsum:");
        System.out.println(generateLoremIpsum(150));

        System.out.println("\nParagraph (with punctuation):");
        System.out.println(generateLoremParagraph(3, 5, 10, 30));
    }

    /**
     * Generates random Lorem Ipsum text with approximately wordCount words
     * @param wordCount approximate number of words to generate
     * @return generated Lorem Ipsum text
     */
    public static String generateLoremIpsum(int wordCount) {
        StringBuilder sb = new StringBuilder();

        // Capitalize first letter of first word
        if (LOREM_WORDS.length > 0) {
            String firstWord = capitalize(LOREM_WORDS[0]);
            sb.append(firstWord);
        }

        // Add remaining words
        for (int i = 1; i < wordCount; i++) {
            String word = LOREM_WORDS[random.nextInt(LOREM_WORDS.length)];
            if (i == 0) {
                word = capitalize(word);
            }
            sb.append(" ").append(word);

            // Occasionally add a comma
            if (random.nextInt(10) == 0 && i < wordCount - 1) {
                sb.append(",");
            }
        }

        // Add period at the end
        sb.append(".");

        return sb.toString();
    }

    /**
     * Generates a paragraph of Lorem Ipsum text with multiple sentences
     * @param minSentences minimum number of sentences
     * @param maxSentences maximum number of sentences
     * @param minWords minimum words per sentence
     * @param maxWords maximum words per sentence
     * @return generated paragraph
     */
    public static String generateLoremParagraph(int minSentences, int maxSentences,
                                                int minWords, int maxWords) {
        int sentenceCount = minSentences + random.nextInt(maxSentences - minSentences + 1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sentenceCount; i++) {
            int words = minWords + random.nextInt(maxWords - minWords + 1);
            sb.append(generateLoremIpsum(words));

            // Add space between sentences, but not after last one
            if (i < sentenceCount - 1) {
                sb.append(" ");
            }
        }

        return sb.toString();
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}