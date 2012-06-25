/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bsc.jaxrs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import biz.source_code.miniTemplator.MiniTemplator;

/**
 *
 * @author softphone
 * 
 * 
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
//@SupportedAnnotationTypes( {"javax.ws.rs.Path"})
@SupportedAnnotationTypes( {"javax.ws.rs.GET", "javax.ws.rs.PUT", "javax.ws.rs.POST", "javax.ws.rs.DELETE"})
@SupportedOptions( {"subfolder", "filepath"})
public class JAXRSWikiProcessor extends AbstractProcessor {

    private static final String SERVICE_NAME_VAR = "service.name";
	private static final String FILEPATH_OPTION = "filepath";
	private static final String SUBFOLDER_OPTION = "subfolder";
	private static final String SERVICES_BLOCK = "services";
	private static final String SERVICE_SECURITY_VAR = "service.security";
	private static final String SERVICE_NOTES_VAR = "service.notes";
	private static final String SERVICE_PATH_VAR = "service.path";
	private static final String SERVICE_CONSUMES_VAR = "service.consumes";
	private static final String SERVICE_PRODUCES_VAR = "service.produces";
	private static final String SERVICE_VERB_VAR = "service.verb";
	private static final String SERVICE_VERSION_VAR = "service.version";
	private static final String SERVICE_DESCRIPTION_VAR = "service.description";

	protected void info( String msg ) {
        processingEnv.getMessager().printMessage(Kind.NOTE, msg );
    }

    protected void warn( String msg ) {
        //logger.warning(msg);
        processingEnv.getMessager().printMessage(Kind.WARNING, msg );
    }

    protected void warn( String msg, Throwable t ) {
        //logger.log(Level.WARNING, msg, t );
        processingEnv.getMessager().printMessage(Kind.WARNING, msg );
        t.printStackTrace(System.err);
    }

    protected void error( String msg ) {
        //logger.severe(msg);
        processingEnv.getMessager().printMessage(Kind.ERROR, msg );
    }

    protected void error( String msg, Throwable t ) {
        //logger.log(Level.SEVERE, msg, t );
        processingEnv.getMessager().printMessage(Kind.ERROR, msg );
        t.printStackTrace(System.err);
    }

    /**
     * 
     * @param value
     * @return
     */
    private String escape( String value ) {
    	
    	return value.replace("{", "\\{")
    				.replace("}", "\\}")
    				.replace("[", "\\[")
    				.replace("]", "\\]")
    				.replace(":)", "\\:\\)");
    }
    
     /**
     * 
     * @param filer
     * @return
     * @throws IOException
     */
    protected FileObject getResourceFormClassPath(Filer filer, final String resource, final String packageName) throws IOException {
        FileObject f = filer.getResource(StandardLocation.CLASS_PATH, packageName, resource);

        //java.io.Reader r = f.openReader(true);  // ignoreEncodingErrors 
        java.io.InputStream is = f.openInputStream();

        if( is==null ) {
            warn( String.format("resource [%s] not found!", resource) );
            return null;
        }
        
        return f;
    }
    
    
    /**
     * 
     * @param subfolder subfolder (e.g. confluence)
     * @param filePath relative path (e.g. children/file.wiki)
     * @return
     * @throws IOException 
     */
    protected FileObject getOutputFile( Filer filer, String subfolder, String filePath ) throws IOException {
        
    	Element e = null;
    	FileObject res = 
        		filer.createResource(StandardLocation.SOURCE_OUTPUT, 
        								subfolder, 
        								filePath, 
        								e);

        return res;
    }
    
    /**
     * 
     * @param e
     * @return
     * @throws ClassNotFoundException
     */
    protected Class<?> getClassFromElement( Element e ) throws ClassNotFoundException {
    	if( null==e ) throw new IllegalArgumentException("e is null!");
    	if( ElementKind.CLASS!=e.getKind() ) throw new IllegalArgumentException( String.format("element [%s] is not a class!", e));
    	
    	TypeElement te = (TypeElement) e;
    	
    	info( String.format("loading class [%s]", te.getQualifiedName().toString()));
    	
    	return Class.forName(te.getQualifiedName().toString());
    	
    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver())      return false;

        java.util.Map<String,String> optionMap = processingEnv.getOptions();
        
        java.net.URL template = getClass().getClassLoader().getResource("ConfluenceWikiTemplate.txt");

        if( template==null ) {
            error( "no template found!");
            return false;
        }

        try {
			final MiniTemplator t = new MiniTemplator( template );
            
        	for( TypeElement e : annotations ) {
    	     	
            	for (Element re : roundEnv.getElementsAnnotatedWith(e)) {
          		
            		if( re.getKind()==ElementKind.METHOD) {

            			//info( String.format("[%s], Element [%s] is [%s] ", re.getEnclosingElement(), re.getKind(), re.getSimpleName()));

            			processService(t, (ExecutableElement) re);
            			
            			t.addBlock(SERVICES_BLOCK);
            		}
                }
            }
        	
            final Filer filer = processingEnv.getFiler();

            String subfolder = optionMap.get(SUBFOLDER_OPTION);
            if( subfolder==null ) {
            	warn("option 'subfolder' has not been provided. Default is used!");
            	subfolder = "";
            }

            
            String filePath = optionMap.get(FILEPATH_OPTION);
            if( filePath==null ) {
            	String msg = "option 'filepath' is mandatory!";
				error(msg);
            	throw new IllegalArgumentException(msg);
            }
            		
            
            FileObject res = getOutputFile(filer, subfolder, filePath);
            
            java.io.Writer w = res.openWriter();
            
            t.generateOutput(w);
            
            w.close();
	
		} catch (Exception e) {
			error("error processing template", e);
			return false;
		}
        
        return true;
    }
    
    /**
	
	<!-- $BeginBlock services -->| *Description:* | ${service.description} |
	| *Since:* |  ${service.version} |
	| *Notes:* |  ${service.notes} |
	| *Security:* |  ${service.security} |
	| *Usage:* | ${service.verb} ${service.path} | 
	| *Consumes* | ${service.consumes} |
	| *Produces:* | ${service.produces} |

     * 
     * @param theClass
     * @param ee
     */
	private void processService(MiniTemplator t, ExecutableElement ee) {
		
		Element enclosingEement = ee.getEnclosingElement();
		
		javax.ws.rs.Path path = enclosingEement.getAnnotation(javax.ws.rs.Path.class);
		
		javax.ws.rs.Path subPath = ee.getAnnotation(javax.ws.rs.Path.class);
		
		
		t.setVariable(SERVICE_NAME_VAR, ee.getSimpleName().toString(), false);
		
		t.setVariable(SERVICE_DESCRIPTION_VAR, "", true);
		t.setVariable(SERVICE_VERSION_VAR, "", true);
		
		{
			Deprecated deprecated = ee.getAnnotation(Deprecated.class);
			if( deprecated!=null ) {
				t.setVariable(SERVICE_NOTES_VAR, "DEPRECATED", true);		
			}
			else {
				t.setVariable(SERVICE_NOTES_VAR, "", true);
			}
		}
		t.setVariable(SERVICE_SECURITY_VAR, "", true);

		{
			Object verb;
			
			verb = ee.getAnnotation(javax.ws.rs.GET.class);
			if( verb != null ) t.setVariable(SERVICE_VERB_VAR, "GET", false);
			else {
				verb = ee.getAnnotation(javax.ws.rs.POST.class);
				if( verb != null ) t.setVariable(SERVICE_VERB_VAR, "POST", false);
				else {

					verb = ee.getAnnotation(javax.ws.rs.PUT.class);
					if( verb != null ) t.setVariable(SERVICE_VERB_VAR, "PUT", false);
					else {

						verb = ee.getAnnotation(javax.ws.rs.DELETE.class);
						if( verb != null ) t.setVariable(SERVICE_VERB_VAR, "DELETE", false);
					
					}	
				}
			}
		}
		
		{
			
			Produces produces = ee.getAnnotation(Produces.class);
			
			if( produces!= null ) {

				String value[] = produces.value();
				
				t.setVariableOpt(SERVICE_PRODUCES_VAR, escape(Arrays.asList(value).toString()) );
				
			}
		
		}

		
		{
			
			Consumes consumes = ee.getAnnotation(Consumes.class);
			
			if( consumes!= null ) {
	
				String value[] = consumes.value();
				
				t.setVariableOpt(SERVICE_CONSUMES_VAR, escape(Arrays.asList(value).toString()));
				
			}
		}
		
		{
			StringBuilder sb = new StringBuilder();
			sb.append(path.value());
			if( subPath!=null ) sb.append('/').append(subPath.value());

			t.setVariable(SERVICE_PATH_VAR, escape(sb.toString()), false);
			
			java.util.Map<String,String> vars = t.getVariables();
			
			
			info( String.format("service [%s] verb [%s] path [%s] consumes [%s] produces [%s]", 
						vars.get(SERVICE_NAME_VAR), 
						vars.get(SERVICE_VERB_VAR),  
						vars.get(SERVICE_PATH_VAR),
						vars.get(SERVICE_CONSUMES_VAR),
						vars.get(SERVICE_PRODUCES_VAR)
						));
		}
		
		
		for(  VariableElement ve : ee.getParameters()) {
			

			DefaultValue dv = ve.getAnnotation(DefaultValue.class);

			info( String.format("param [%s] [%s]", ve.getSimpleName(), dv));
			
			QueryParam qp = ve.getAnnotation(QueryParam.class);
			if( qp != null ) {
				t.setVariableOpt("param.name", qp.value());
				t.setVariableOpt("param.default", (dv!=null) ?dv.value() : "");
				t.addBlock("parameters");
			}
			else {
				FormParam fp = ve.getAnnotation(FormParam.class);
				if( fp!= null ) {
					t.setVariableOpt("param.name", fp.value());			
					t.setVariableOpt("param.default", (dv!=null) ?dv.value() : "");
					t.addBlock("parameters");
				}
			}
			
		}
	}
		


}
