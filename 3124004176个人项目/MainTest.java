import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Main 单元测试：自研轻量测试运行器（纯 JDK 标准库实现，无第三方依赖）
 * 编译: javac -encoding UTF-8 Main.java MainTest.java
 * 运行: java -cp . MainTest
 */
public class MainTest {

    /** 相似度断言允许的最大误差 */
    private static final double EPSILON = 0.005;

    private static int passed = 0;
    private static int failed = 0;

    /** 测试用例函数式接口 */
    private interface TestCase {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        run("testIdentical .................... ", MainTest::testIdentical);
        run("testCompletelyDifferent ......... ", MainTest::testCompletelyDifferent);
        run("testPartialPlagiarism ........... ", MainTest::testPartialPlagiarism);
        run("testOriginalEmpty ............... ", MainTest::testOriginalEmpty);
        run("testCopyEmpty ................... ", MainTest::testCopyEmpty);
        run("testBothEmpty ................... ", MainTest::testBothEmpty);
        run("testPunctuationOnly ............. ", MainTest::testPunctuationOnly);
        run("testLongText .................... ", MainTest::testLongText);
        run("testEnglishCase ................. ", MainTest::testEnglishCase);
        run("testSpecialCharsAndNewlines ..... ", MainTest::testSpecialCharsAndNewlines);
        run("testFileNotFound ................ ", MainTest::testFileNotFound);
        run("testReadError ................... ", MainTest::testReadError);

        long cost = System.currentTimeMillis() - start;
        System.out.println("------------------------------------------------------------");
        System.out.println("Ran 12 tests in " + cost + "ms");
        System.out.println();
        System.out.println(failed == 0 ? "OK" : "FAILED (failures=" + failed + ")");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** 执行单个用例并打印 unittest 风格日志 */
    private static void run(String name, TestCase testCase) {
        long t0 = System.nanoTime();
        try {
            testCase.run();
            passed++;
            System.out.println(name + "ok (" + (System.nanoTime() - t0) / 1000000 + "ms)");
        } catch (Throwable e) {
            failed++;
            System.out.println(name + "FAIL: " + e.getMessage());
        }
    }

    /** 断言实际值与期望值误差在 EPSILON 以内 */
    private static void assertRate(double expected, double actual) {
        if (Math.abs(expected - actual) > EPSILON) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    /** 完全相同：相似度应为 1.00 */
    private static void testIdentical() {
        String s = "今天是星期天，天气晴，今天晚上我要去看电影。";
        assertRate(1.00, Main.calculateSimilarity(s, s));
    }

    /** 完全不同：相似度应为 0.00 */
    private static void testCompletelyDifferent() {
        String orig = "今天是星期天，天气晴，今天晚上我要去看电影。";
        String copy = "机器学习是人工智能的一个重要分支领域。";
        assertRate(0.00, Main.calculateSimilarity(orig, copy));
    }

    /** 部分抄袭：相似度应在 0.0 到 1.0 之间 */
    private static void testPartialPlagiarism() {
        String orig = "今天是星期天，天气晴，今天晚上我要去看电影。";
        String copy = "今天是周天，天气晴朗，我晚上要去看电影。";
        double rate = Main.calculateSimilarity(orig, copy);
        if (rate <= 0.0 || rate >= 1.0) {
            throw new AssertionError("部分抄袭结果应介于 0 和 1 之间，实际 " + rate);
        }
        assertRate(0.61, rate);
    }

    /** 原文为空：相似度应为 0.00 */
    private static void testOriginalEmpty() {
        assertRate(0.00, Main.calculateSimilarity("", "今天天气晴"));
    }

    /** 抄袭文为空：相似度应为 0.00 */
    private static void testCopyEmpty() {
        assertRate(0.00, Main.calculateSimilarity("今天天气晴", ""));
    }

    /** 两者都为空：相似度应为 1.00 */
    private static void testBothEmpty() {
        assertRate(1.00, Main.calculateSimilarity("", ""));
    }

    /** 纯空格或纯标点符号：归一化后均为空，相似度应为 1.00 */
    private static void testPunctuationOnly() {
        assertRate(1.00, Main.calculateSimilarity("，。！？　 …", "、；：「」\n\t"));
        assertRate(0.00, Main.calculateSimilarity("，。！", "今天天气晴"));
    }

    /** 超长文本：验证结果稳定性与性能 */
    private static void testLongText() {
        String origUnit = "今天是星期天，天气晴，今天晚上我要去看电影。";
        String copyUnit = "今天是周天，天气晴朗，我晚上要去看电影。";
        StringBuilder orig = new StringBuilder();
        StringBuilder copy = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            orig.append(origUnit);
            copy.append(copyUnit);
        }
        long t0 = System.nanoTime();
        double rate = Main.calculateSimilarity(orig.toString(), copy.toString());
        long cost = (System.nanoTime() - t0) / 1000000;
        assertRate(0.64, rate);
        if (cost > 5000) {
            throw new AssertionError("超长文本计算超时: " + cost + "ms");
        }
    }

    /** 英文大小写区分：全大写与全小写应判为完全不重复 */
    private static void testEnglishCase() {
        assertRate(0.00, Main.calculateSimilarity("HELLO WORLD", "hello world"));
        assertRate(1.00, Main.calculateSimilarity("HELLO WORLD", "HELLO WORLD"));
    }

    /** 特殊字符和换行符：归一化后不影响结果 */
    private static void testSpecialCharsAndNewlines() {
        String s1 = "今天是星期天，\n天气晴。\r\n今天晚上我要去看电影！";
        String s2 = "今天是星期天天气晴今天晚上我要去看电影";
        assertRate(1.00, Main.calculateSimilarity(s1, s2));
    }

    /** 文件不存在：readFile 应抛出 IOException */
    private static void testFileNotFound() throws Exception {
        expectIOException("D:/path/not_exist_12345.txt");
    }

    /** 读取文件发生异常：目标是目录时 readFile 应抛出 IOException */
    private static void testReadError() throws Exception {
        expectIOException(System.getProperty("java.io.tmpdir"));
    }

    /** 通过反射调用私有方法 readFile，断言其抛出 IOException */
    private static void expectIOException(String path) throws Exception {
        Method m = Main.class.getDeclaredMethod("readFile", String.class);
        m.setAccessible(true);
        try {
            m.invoke(null, path);
            throw new AssertionError("期望抛出 IOException，但未抛出");
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof IOException)) {
                throw new AssertionError("期望 IOException，实际 " + e.getCause());
            }
        }
    }
}
