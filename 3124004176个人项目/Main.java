import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 论文查重程序
 * 用法: java Main <原文文件绝对路径> <抄袭版文件绝对路径> <答案文件绝对路径>
 * 算法: 文本预处理后提取字符二元组(bigram)词频向量，计算余弦相似度作为重复率
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("用法: java Main <原文文件绝对路径> <抄袭版文件绝对路径> <答案文件绝对路径>");
            System.exit(1);
        }
        try {
            String original = readFile(args[0]);
            String plagiarized = readFile(args[1]);
            double rate = calculateSimilarity(original, plagiarized);
            String result = String.format(Locale.ROOT, "%.2f", rate);
            Files.write(Paths.get(args[2]), result.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("文件读写失败: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 读取文件内容，优先按 UTF-8 解码，失败则回退为 GBK
     */
    private static String readFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    /**
     * 计算两篇文本的重复率，返回值范围 [0.0, 1.0]
     */
    static double calculateSimilarity(String s1, String s2) {
        String t1 = normalize(s1);
        String t2 = normalize(s2);
        if (t1.isEmpty() || t2.isEmpty()) {
            // 两篇均为空视为完全相同，仅一篇为空视为完全不同
            return (t1.isEmpty() && t2.isEmpty()) ? 1.0 : 0.0;
        }

        Map<String, Integer> v1 = bigramFrequency(t1);
        Map<String, Integer> v2 = bigramFrequency(t2);

        long dot = 0L;
        for (Map.Entry<String, Integer> entry : v1.entrySet()) {
            Integer count = v2.get(entry.getKey());
            if (count != null) {
                dot += (long) entry.getValue() * count;
            }
        }
        double norm1 = 0.0;
        for (int count : v1.values()) {
            norm1 += (double) count * count;
        }
        double norm2 = 0.0;
        for (int count : v2.values()) {
            norm2 += (double) count * count;
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 去除标点、空白等噪声字符，仅保留字母与数字（含中文字符）
     */
    private static String normalize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 统计相邻两个字符构成的 bigram 词频；单字符文本退化为 unigram
     */
    private static Map<String, Integer> bigramFrequency(String s) {
        Map<String, Integer> freq = new HashMap<>();
        if (s.length() == 1) {
            freq.put(s, 1);
            return freq;
        }
        for (int i = 0; i < s.length() - 1; i++) {
            freq.merge(s.substring(i, i + 2), 1, Integer::sum);
        }
        return freq;
    }
}
