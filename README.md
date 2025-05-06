Bingo
=====

A small framework to facilate downloading or using a bundled tar file with an executable for a specific platform

Usage you need to implement 2 interfaces: `FilenameMapper` to get the download url and file names and `ProcessParameters` to add parameters to the process to run

Example for this with esbuild downloaded from npm:
```java
public class EsBuildFilenameMapper implements FilenameMapper {
    public static final String ESBUILD_TGZ_PATH_TEMPLATE = "%1$s-%2$s.tgz";
    public static final String ESBUILD_URL_TEMPLATE = "https://registry.npmjs.org/@esbuild/%1$s/-/";

    @Override
    public String downloadUrl(String version, String classifier) {
        final String tgz = tarFileName(version, classifier);
        return ESBUILD_URL_TEMPLATE.formatted(classifier) + tgz;
    }

    @Override
    public String tarFileName(String version, String classifier) {
        return ESBUILD_TGZ_PATH_TEMPLATE.formatted(classifier, version);
    }

    @Override
    public String executable() {
        return "package/bin/esbuild";
    }
}
```

Then call the resolver to either use one in the bundle or download:
```java
final Path path = Resolver.create(new EsBuildFilenameMapper()).resolve(defaultVersion);
```

Then you can execute by:

```java
String workingDirectory = System.getProperty("user.dir");
final ExecuteResult executeResult = new Execute(Paths.get(workingDirectory), path.toFile(), new EsBuildParameters())
         .executeAndWait();
```
