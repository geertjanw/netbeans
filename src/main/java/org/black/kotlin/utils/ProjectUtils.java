package org.black.kotlin.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.black.kotlin.model.KotlinEnvironment;
import org.black.kotlin.project.KotlinProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author Александр
 */
public class ProjectUtils {

    private static final String LIB_FOLDER = "lib";
    private static final String LIB_EXTENSION = "jar";
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String KT_HOME;

    static {
        if (System.getenv("KOTLIN_HOME") != null) {
            KT_HOME = System.getenv("KOTLIN_HOME") + FILE_SEPARATOR;
        } else {
            KT_HOME = System.getenv("KT_HOME") + FILE_SEPARATOR;
        }
    }

    /**
     * Finds java file with main method.
     *
     * @param files project files
     * @return file with main method.
     * @throws IOException
     */
    public static FileObject findJavaMain(FileObject[] files) throws IOException {
        for (FileObject file : files) {
            if (!file.isFolder()) {
                for (String line : file.asLines()) {
                    if (line.contains("public static void main(")) {
                        return file;
                    }
                }
            } else {
                FileObject main = findJavaMain(file.getChildren());
                if (main != null) {
                    return main;
                }
            }
        }
        return null;
    }
    
    private static void makeFileCollection(FileObject[] files, List<FileObject> collection) {
        for (FileObject file : files) {
            if (!file.isFolder()) {
                collection.add(file);
            } else {
                makeFileCollection(file.getChildren(), collection);
            }
        }
    }

    public static KtFile findKotlinMain(FileObject[] files) throws IOException {
        List<KtFile> ktFiles = new ArrayList<KtFile>();
        List<FileObject> collection = new ArrayList<FileObject>();
        makeFileCollection(files, collection);
        for (FileObject file : collection) {
            if (!file.isFolder()) {
                ktFiles.add(KotlinEnvironment.parseFile(FileUtil.toFile(file)));
            }
        }

        return KtMainDetector.getMainFunctionFile(ktFiles);
    }

    public static String getMainFileClass(FileObject[] files) throws IOException {
        KtFile main = findKotlinMain(files);
        if (main != null) {
            String name = main.getName().split(".kt")[0];
            String path = main.getViewProvider().getVirtualFile().getCanonicalPath();
            if (path != null) {
                InputStream is = new FileInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String beginning = br.readLine().split(" ")[1].split(";")[0];
                is.close();

                StringBuilder builder = new StringBuilder("");

                builder.append(beginning).append(".");
                char firstCharUpperCase = Character.toUpperCase(name.charAt(0));
                StringBuilder mainFileClass = new StringBuilder("");
                mainFileClass.append(firstCharUpperCase);
                for (int i = 1; i < name.length(); i++) {
                    mainFileClass.append(name.charAt(i));
                }
                mainFileClass.append("Kt");
                builder.append(mainFileClass.toString());
                return builder.toString();
            }
        } else {
            FileObject javaMain = findJavaMain(files);
            if (javaMain != null) {
                String name = javaMain.getName();
                String path = javaMain.getPath();
                InputStream is = new FileInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
                String beginning = br.readLine().split(" ")[1].split(";")[0];
                is.close();
                return beginning + "." + name;
            }
        }
        return null;
    }

    /**
     * returns path to output directory.
     *
     * @param proj Kotlin project
     * @return path to output directory
     */
    public static String getOutputDir(KotlinProject proj) {

        File path = new File(proj.getProjectDirectory().getPath() + FILE_SEPARATOR + "build");

        if (!path.exists()) {
            if (!path.mkdirs()) {
                System.err.println("Cannot create a directory");
            }
        }

        String dir = proj.getProjectDirectory().getPath() + FILE_SEPARATOR + "build";
        String[] dirs;
        if (FILE_SEPARATOR.equals("\\")) {
            dirs = dir.split("\\\\");
        } else {
            dirs = dir.split(FILE_SEPARATOR);
        }
        StringBuilder outputDir = new StringBuilder("");

        for (String str : dirs) {
            outputDir.append(str);
            outputDir.append(FILE_SEPARATOR);
        }
        outputDir.append(proj.getProjectDirectory().getName()).append(".jar");

        return outputDir.toString();
    }

    public static void clean(KotlinProject proj) {

        try {
            if (proj.getProjectDirectory().getFileObject("build") != null) {
                proj.getProjectDirectory().getFileObject("build").delete();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    @NotNull
    public static List<String> getClasspath() {
        return new ArrayList<String>(KotlinClasspath.getKotlinClasspath());
    }

    public static List<String> getLibs(KotlinProject proj) {
        List<String> libs = new ArrayList<String>();
        FileObject libFolder = proj.getProjectDirectory().getFileObject("lib");
        for (FileObject fo : libFolder.getChildren()) {
            if (fo.hasExt("jar")) {
                libs.add(fo.getNameExt());
            }
        }
        return libs;
    }

    public static String buildLibPath(String libName) {
        return KT_HOME + buildLibName(libName);
    }

    private static String buildLibName(String libName) {
        return LIB_FOLDER + FILE_SEPARATOR + libName + "." + LIB_EXTENSION;
    }

}
