package io.mvnpm.process;

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
