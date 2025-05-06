package io.mvnpm.process;

public interface FilenameMapper {

    /**
     * Return the url based on the version and the classifier.
     *
     * @param version    the version of the program you want
     * @param classifier based on the platform and the architecture.
     * @return the url as a string to fetch the tar file.
     */
    String downloadUrl(String version, String classifier);

    /**
     * The tar file of the program you want to run.
     *
     * @param version
     * @param classifier
     * @return the name of the tar file that has been downloaded
     */
    String tarFileName(String version, String classifier);

    /**
     * The name / location of the executable in the extracted tar file.
     *
     * @return the location and name of the program to execute
     */
    String executable();
}
