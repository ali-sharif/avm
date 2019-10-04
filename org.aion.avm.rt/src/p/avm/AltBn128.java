package p.avm;

import s.java.lang.Object;
import i.IInstrumentation;
import org.aion.avm.RuntimeMethodFeeSchedule;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

@SuppressWarnings("SpellCheckingInspection")
public class AltBn128 extends Object {

    @SuppressWarnings("unused")
    public static class DetectPlatform {
        private String os;
        private String arch;

        public DetectPlatform() {
            // resolve OS
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                this.os = "win";
            } else if (osName.contains("linux")) {
                this.os = "linux";
            } else if (osName.contains("mac")) {
                this.os = "mac";
            } else {
                throw new RuntimeException("unrecognized os: " + osName);
            }

            // resolve architecture
            String osArch = System.getProperty("os.arch").toLowerCase();
            if (osArch.contains("amd64") || osArch.contains("x86_64")) {
                this.arch = "amd64";
            } else {
                throw new RuntimeException("unrecognized architecture: " + osName);
            }
        }

        public String getOs() { return os; }

        public String getArch() { return arch; }

        public String toString() {
            return os + "_" + arch;
        }
    }

    public static class NativeLoader {
        private static File buildPath(String... args) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                sb.append(File.separator);
                sb.append(arg);
            }

            return sb.length() > 0 ? new File(sb.substring(1)) : new File(".");
        }

        public static void loadLibrary(String module) {
            File dir = buildPath("native", new DetectPlatform().toString(), module);

            try (Scanner s = new Scanner(new File(dir, "file.list"))) {
                while (s.hasNextLine()) {
                    String line = s.nextLine();

                    if (line.startsWith("/") || line.startsWith(".")) { // for debug
                        // purpose
                        // mainly
                        System.load(line);
                    } else {
                        System.load(new File(dir, line).getCanonicalPath());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load libraries for " + module, e);
            }
        }
    }

    private static class AltBn128Jni {
        public native byte[] altBn128Add(byte[] point1, byte[] point2);
        public native byte[] altBn128Mul(byte[] point, byte[] scalar);
        public native boolean altBn128Pair(byte[] g1_point_list, byte[] g2_point_list);
    }

    // load native libraries
    static {
        NativeLoader.loadLibrary("altbn128");
    }

    @SuppressWarnings("unused")
    private static final class Holder {
        public static final AltBn128Jni INSTANCE = new AltBn128Jni();
    }

    // non-instantiable class
    private AltBn128() { }

    // metering constants
    public static final long RT_BN_METHOD_FEE_PAIRING_BASE = 45_000;
    public static final long RT_BN_METHOD_FEE_PAIRING_INSTANCE = 34_000;

    public static final long AltBn128_avm_g1EcAdd = RuntimeMethodFeeSchedule.RT_METHOD_FEE_LEVEL_2; // benchmark per op ~ 7us
    public static final long AltBn128_avm_g1EcMul = RuntimeMethodFeeSchedule.RT_METHOD_FEE_LEVEL_6; // benchmark per op ~ 230us

    public static final long AltBn128_avm_pairingCheck_base = RT_BN_METHOD_FEE_PAIRING_BASE; // benchmark per op ~ 3ms (1 op) - 1.4ms (amortized over 10 ops)
    public static final long AltBn128_avm_pairingCheck_per_pairing = RT_BN_METHOD_FEE_PAIRING_INSTANCE;

    private static int WORD_SIZE = 32;
    // points in G1 are encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
    private static int G1_POINT_SIZE = 2 * WORD_SIZE;
    // points in G2, encoded like so: [p1[0].x || p1[0].y || p1[1].x || p2[1].y || p2[0].x]. Each coordinate is 32-byte aligned.
    private static int G2_POINT_SIZE = 4 * WORD_SIZE;

    // Runtime-facing implementation
    /**
     * Computes EC addition in G1
     *
     * @param point1 point in G1, encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
     * @param point2 point in G1, encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
     *
     */
    public static byte[] avm_g1EcAdd(byte[] point1, byte[] point2) {
        // assert valid data.
        assert (point1 != null && point2 != null &&
                point1.length == G1_POINT_SIZE && point2.length == G1_POINT_SIZE);

        // gas costing
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(AltBn128_avm_g1EcAdd);

        // call jni
        return Holder.INSTANCE.altBn128Add(point1, point2);
    }

    /**
     * Computes scalar multiplication in G1
     *
     * @param point point in G1, encoded like so: [p.x || p.y]. Each coordinate is 32-byte aligned.
     * @param scalar natural number (> 0), byte aligned to 32 bytes.
     */
    public static byte[] avm_g1EcMul(byte[] point, byte[] scalar) {
        // assert valid data.
        assert (point != null && scalar != null &&
                point.length == G1_POINT_SIZE && scalar.length == WORD_SIZE);

        // gas costing
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(AltBn128_avm_g1EcMul);

        // call jni
        return Holder.INSTANCE.altBn128Mul(point, scalar);
    }

    /**
     * The Pairing itself is a transformation of the form G1 x G2 -> Gt, <br/>
     * where Gt is a subgroup of roots of unity in Fp12 field<br/>
     * <br/>
     *
     * Pairing Check input is a sequence of point pairs, the result is either success (true) or failure (false) <br/>
     * <br/>
     *
     * @param g1_point_list list of points in G1, encoded like so: [p1.x || p1.y || p2.x || p2.y || ...].
     *                      Each coordinate is byte aligned to 32 bytes.
     * @param g2_point_list list of points in G2, encoded like so: [p1[0].x || p1[0].y || p1[1].x || p2[1].y || p2[0].x || ...].
     *                      Each coordinate is byte aligned to 32 bytes.
     *
     */
    public static boolean avm_pairingCheck(byte[] g1_point_list, byte[] g2_point_list) {
        // assert valid data.
        assert (g1_point_list != null && g2_point_list != null &&
                g1_point_list.length % G1_POINT_SIZE == 0 && g2_point_list.length % G2_POINT_SIZE == 0); // data is well-aligned
        int g1_list_size = g1_point_list.length / G1_POINT_SIZE;
        int g2_list_size = g2_point_list.length / G2_POINT_SIZE;
        assert (g1_list_size == g2_list_size);

        // gas costing: the list size tells you how many pairing operations will be performed
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(
                AltBn128_avm_pairingCheck_base + g1_list_size * AltBn128_avm_pairingCheck_per_pairing);

        // call jni
        return Holder.INSTANCE.altBn128Pair(g1_point_list, g2_point_list);

    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
