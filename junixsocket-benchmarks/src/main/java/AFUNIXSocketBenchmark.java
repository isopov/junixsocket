

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.AverageTime)
@Fork(1)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class AFUNIXSocketBenchmark {

    private AFUNIXServerSocket server;
    public AFUNIXSocketAddress address;
    private byte[] bytes;

    @Param({ "1", "100" })
    public int payload;

    @Setup
    public void setup() throws IOException {

        // File file = File.createTempFile("afunix", ".sock");
        final File file = new File(new File(System.getProperty("java.io.tmpdir")), "junixsocket-test.sock");

        address = new AFUNIXSocketAddress(file);
        server = AFUNIXServerSocket.newInstance();
        server.bind(address);

        bytes = new byte[payload];
        new Random().nextBytes(bytes);

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] readBytes = new byte[payload];
                while (true) {
                    try (Socket sock = server.accept();
                            InputStream is = sock.getInputStream()) {
                        is.read(readBytes);
                    } catch (IOException e) {
                        return;
                    }
                }
            }
        }).start();

    }

    @TearDown
    public void tearDown() throws IOException {
        server.close();

    }

    @Benchmark
    public void write() throws IOException {
        try (AFUNIXSocket client = AFUNIXSocket.newInstance()) {
            client.connect(address);
            try (OutputStream os = client.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(".*" + AFUNIXSocketBenchmark.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

}
