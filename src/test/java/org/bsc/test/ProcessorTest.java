package org.bsc.test;

import com.thoughtworks.qdox.JavaDocBuilder;
import java.net.URISyntaxException;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ProcessorTest 
{
	
	
	@Test 
        public void testURI() throws URISyntaxException {
		
		
		java.net.URI templateUriClasspath = new java.net.URI( "classpath:/src/test/resources/template.txt");
		
		
		Assert.assertThat( templateUriClasspath.getScheme(), Is.is("classpath"));
		
		Assert.assertThat( templateUriClasspath.getPath(), Is.is("/src/test/resources/template.txt"));
		
		java.net.URI templateUriFile = new java.net.URI( "file:///src/test/resources/template.txt");
		
		
		Assert.assertThat( templateUriFile.getScheme(), Is.is("file"));
		
		Assert.assertThat( templateUriFile.getPath(), Is.is("/src/test/resources/template.txt"));
	}
        
	@Test 
        public void testDoclet()  {
    
            JavaDocBuilder builder = new JavaDocBuilder();
	
		
	}
}
