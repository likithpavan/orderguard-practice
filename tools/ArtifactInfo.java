import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import java.util.jar.JarFile;

class ArtifactInfo {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: java tools/ArtifactInfo.java path/to/orderguard.jar");
        Path jar = Path.of(args[0]);
        Properties build = new Properties();
        try (JarFile file = new JarFile(jar.toFile()); var input = file.getInputStream(file.getJarEntry("build-info.properties"))) {
            build.load(input);
        }
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        System.out.println("version=" + build.getProperty("version"));
        System.out.println("commit=" + build.getProperty("commit"));
        System.out.println("sha256=" + hash);
    }
}
