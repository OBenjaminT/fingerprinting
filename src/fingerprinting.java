import java.util.DoubleSummaryStatistics;
import java.util.stream.DoubleStream;

public class fingerprinting {
    public static void main(String[] args) {
        DoubleSummaryStatistics list;
        list = DoubleStream.iterate(0, i -> i + 1)
                .map(i -> Math.pow(i, 2))
                .limit(999).summaryStatistics();
        System.out.println(list);
    }
}
