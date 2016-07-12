package org.bsc.test;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.BeanProperty;
import com.thoughtworks.qdox.model.JavaClass;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ProcessorTest {

    @Test
    public void testURI() throws URISyntaxException {

        final java.net.URI templateUriClasspath = new java.net.URI("classpath:/src/test/resources/template.txt");

        Assert.assertThat(templateUriClasspath.getScheme(), Is.is("classpath"));

        Assert.assertThat(templateUriClasspath.getPath(), Is.is("/src/test/resources/template.txt"));

        final java.net.URI templateUriFile = new java.net.URI("file:///src/test/resources/template.txt");

        Assert.assertThat(templateUriFile.getScheme(), Is.is("file"));

        Assert.assertThat(templateUriFile.getPath(), Is.is("/src/test/resources/template.txt"));
    }

    @Test
    public void testDoclet() throws IOException {

        final JavaDocBuilder builder = new JavaDocBuilder();

        builder.addSource(
                Paths.get("src", "test", "java",
                        "org", "bsc", "test", "doclet", "POJOSample.java")
                .toFile());

        final JavaClass javaClass = builder.getClassByName("org.bsc.test.doclet.POJOSample");

        Assert.assertThat(javaClass, IsNull.notNullValue());

        final BeanProperty[] properties = javaClass.getBeanProperties();

        Assert.assertThat(properties, IsNull.notNullValue());
        Assert.assertThat(properties.length, IsEqual.equalTo(2));
        Assert.assertThat(properties[0].getName(), IsEqual.equalTo("stringObjVal"));
        Assert.assertThat(properties[1].getName(), IsEqual.equalTo("testing"));

    }
}
