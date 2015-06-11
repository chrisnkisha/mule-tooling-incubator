package org.mule.tooling.devkit.template;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public class ImageWriter {

    private IProject project;

    public ImageWriter(IProject project) {
        this.project = project;
    }

    public void apply(final String templatePath, final String resultPath, IProgressMonitor monitor) {
        try {
            monitor.beginTask("Creating file " + resultPath, 100);
            File f2 = new File(project.getProject().getFile(resultPath).getLocationURI());
            InputStream in = ImageWriter.class.getResource(templatePath).openStream();

            // For Append the file.
            // OutputStream out = new FileOutputStream(f2,true);

            // For Overwrite the file.
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            monitor.worked(30);
            in.close();
            out.flush();
            out.close();
            monitor.worked(70);

        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            monitor.done();
        }
    }
}
