/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bsc.jaxrs;

import biz.source_code.miniTemplator.MiniTemplator;
import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import static java.lang.String.format;

/**
 *
 * @author softphone
 *
 *
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes( {"javax.ws.rs.GET", "javax.ws.rs.PUT", "javax.ws.rs.POST", "javax.ws.rs.DELETE"})
@SupportedOptions( {"subfolder", "filepath", "templateUri"})
public class JAXRSWikiProcessor extends AbstractProcessor {

    private static final String TEMPLATEURI_OPTION = "templateUri";
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
    private static final String SERVICE_SINCE_VAR = "service.since";
    private static final String SERVICE_DESCRIPTION_VAR = "service.description";
    private static final String SERVICE_RESPONSE_VAR = "service.response";
    private static final String SERVICE_RETURNCODE_VAR = "service.return";
    private static final String SERVICE_EXCEPTIONS_VAR = "service.exception";
    private static final String SERVICE_CLASSNAME_VAR = "service.class.name";
    private static final String SERVICE_RESPONSETYPE_VAR = "service.responsetype";
    private static final String SERVICE_PARAMNAME_VAR = "param.name";
    private static final String SERVICE_PARAMTYPE_VAR = "param.type";
    private static final String SERVICE_PARAMDEFAULT_VAR = "param.default";
    private static final String SERVICE_SEE_VAR = "service.see";
    com.sun.source.util.Trees trees;

    protected void info(String fmt, Object... args) {

        processingEnv.getMessager().printMessage(Kind.NOTE, format(fmt, (Object[]) args));
    }

    protected void warn(String fmt, Object... args) {
        processingEnv.getMessager().printMessage(Kind.WARNING, format(fmt, (Object[]) args));
        if( args.length > 0 ) {
            final Object last = args[args.length-1];
            if( last instanceof Throwable ) {
                ((Throwable) last).printStackTrace(System.err);
            }
        }
    }

    protected void error(String fmt, Object... args) {
        processingEnv.getMessager().printMessage(Kind.ERROR, format(fmt, (Object[]) args));
        if (args.length > 0) {
            final Object last = args[args.length - 1];
            if (last instanceof Throwable) {
                ((Throwable) last).printStackTrace(System.err);
            }
        }
    }
    
    /**
     *
     * @param value
     * @return
     */
    private String escape(String value) {

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

        if (is == null) {
            warn("resource [%s] not found!", resource);
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
    protected FileObject getOutputFile(Filer filer, String subfolder, String filePath) throws IOException {

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

        info( "loading class [%s]", te.getQualifiedName().toString());

        return Class.forName(te.getQualifiedName().toString());

    }
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver())      return false;

        java.util.Map<String,String> optionMap = processingEnv.getOptions();
        
        trees = com.sun.source.util.Trees.instance(processingEnv);
        
        java.net.URL template = null;
        
        String templateUri = optionMap.get(TEMPLATEURI_OPTION);
        if( templateUri==null ) {
            info("not template defined. Default is used!");
            template = getClass().getClassLoader().getResource("ConfluenceWikiTemplate.txt");
        }
        else {
        	
            try {
                java.net.URI templateURI = new java.net.URI(templateUri);

                String scheme = templateURI.getScheme();
                String path = templateURI.getPath();

                if (path == null) {
                    String msg = String.format("option '%s' path is null!", TEMPLATEURI_OPTION);
                    error(msg);
                    throw new IllegalArgumentException(msg);
                }

                if ("file".compareToIgnoreCase(scheme) == 0) {

                    info("use template [%s]", path);

                    java.io.File source = new java.io.File(path);

                    template = source.toURI().toURL();

                } else if ("classpath".compareToIgnoreCase(scheme) == 0) {

                    path = (path.startsWith("/")) ? path.substring(1) : path;

                    info("use template [%s]", path);

                    template = getClass().getClassLoader().getResource(path);
                } else {
                    String msg = String.format("option '%s' scheme [%s] not supported!", TEMPLATEURI_OPTION, scheme);
                    error(msg);
                    throw new IllegalArgumentException(msg);

                }

            } catch (URISyntaxException | MalformedURLException e) {
                String msg = String.format("option '%s' path is invalid!", TEMPLATEURI_OPTION);
                error(msg);
                throw new IllegalArgumentException(msg);
            }
        }
        

        if( template==null ) {
            error( "no template found!");
            return false;
        }

        try {
		final MiniTemplator t = new MiniTemplator.Builder()
                                 .setSkipUndefinedVars(true)
                                 .build(template, Charset.defaultCharset());
            
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
     * 
     * @param typeElement
     * @return 
     */
    String getFullClassName( Element typeElement ) {
     
        if( typeElement instanceof TypeElement  ) {
            
            return ((TypeElement)typeElement).getQualifiedName().toString();
        }
        
        return typeElement.getSimpleName().toString();
    }
    
    /**
     * 
     * @param entity
     * @param tagName
     * @param defaultValue
     * @return 
     */
    String getTagByName( AbstractInheritableJavaEntity entity, String tagName, String defaultValue  ) {
        
        DocletTag tag = entity.getTagByName(tagName, true);
        
        if( tag == null) return defaultValue;
        
        return (tag.getValue()!=null) ? tag.getValue() : defaultValue;
    }


    /**
     * 
     * @param tags
     * @param paramName
     * @return 
     */
    DocletTag getParamByName( DocletTag [] tags, String paramName  ) {

        if( tags == null ) return null;
        
        for( DocletTag tag : tags ) {
            
            final String name = tag.getParameters()[0];
            if( name.equals(paramName)){
                return tag;
            }
        }
        
        return null;
    }
    
    /**
     * 
     * @param serviceClass
     * @param methodElement
     * @return 
     */
    JavaMethod getMethod( JavaClass serviceClass, ExecutableElement methodElement ) {
        java.util.List<? extends VariableElement> paramList = methodElement.getParameters();
        
        Type paramTypes[] = new Type[ paramList.size() ];
        
        int i = 0; 
        for( VariableElement ve : paramList ) {                     
            paramTypes[i++] = new Type( ve.getSimpleName().toString());
        }

        JavaMethod method = serviceClass.getMethodBySignature(methodElement.getSimpleName().toString(), paramTypes);
        
        if( method ==null ) {
            final String methodName = methodElement.getSimpleName().toString();
            for( JavaMethod m : serviceClass.getMethods() ) {
                if( methodName.equals(m.getName())) {
                    return m;
                }
            }
        }

        return method;
    }

    /**
     *
     * @param enclosingEement
     */
    void processDocletForElement( MiniTemplator t, Element enclosingElement, ExecutableElement methodElement ) throws IOException {

        final com.sun.source.util.TreePath treePath = trees.getPath(enclosingElement);

        final FileObject sourceFile = treePath.getCompilationUnit().getSourceFile();

        final java.net.URI uri = sourceFile.toUri();

        info( String.format("processing doclet for source[%s]", uri) );

        final JavaDocBuilder builder = new JavaDocBuilder();

        builder.addSource( Paths.get(uri).toFile() );

        final String fqn = getFullClassName(enclosingElement);

        final JavaClass serviceClass = builder.getClassByName( fqn );

        if( serviceClass == null ) {
            warn( String.format("Service Class [%s] in source [%s] not found!. Doclet processing skipped! ", fqn, sourceFile.toUri().toString()));
            return;
        }

        final JavaMethod method = getMethod(serviceClass, methodElement);

        if( method ==null ) {
            warn("Method [%s] of class [%s] in source [%s] not found!. Doclet processing skipped! ",
                    methodElement.getSimpleName().toString(),
                    fqn,
                    sourceFile.toUri().toString());
            return;
        }

        {
            final String comment = method.getComment();
            final String deprecated = getTagByName(method, "deprecated", null);
            final String security = getTagByName(method, "security", "");

            if( deprecated!=null ) {
                t.setVariable(SERVICE_NOTES_VAR, "DEPRECATED " + deprecated, true);

            }
            t.setVariable(SERVICE_CLASSNAME_VAR, method.getParentClass().toString(), false);
            t.setVariable(SERVICE_NAME_VAR, methodElement.getSimpleName().toString(), false);

            t.setVariable(SERVICE_DESCRIPTION_VAR, (comment != null) ? comment : "", true);
            t.setVariable(SERVICE_SINCE_VAR, getTagByName(method, "since", ""), true);

            t.setVariable(SERVICE_SECURITY_VAR, security, true);

            t.setVariableOpt(SERVICE_RESPONSETYPE_VAR, method.getReturnType().getValue());

            populateParamDefault(method.getReturnType(), t, SERVICE_RESPONSE_VAR);

            t.setVariableOpt(SERVICE_RETURNCODE_VAR, getTagByName(method, "return", ""));
            //populate exceptions
            populateExceptions(method.getExceptions(), t, method);
            //populate see
            populateSeeTag(t, method);
            //populate input parameters
            populateInputParameters(t, method);

        }


        // Below code is commented so that we can avoid taking parameters info from documentation comments


        // final DocletTag paramTags[] = method.getTagsByName("param");

//        for (VariableElement paramElement : methodElement.getParameters()) {
//
//            final DefaultValue dv = paramElement.getAnnotation(DefaultValue.class);
//
//            final DocletTag tag = getParamByName(paramTags, paramElement.getSimpleName().toString());
//
//            final String comment = (tag!=null) ? tag.getValue() : null;

//            QueryParam qp = paramElement.getAnnotation(QueryParam.class);
//            if (qp != null) {
//                t.setVariableOpt("param.name", qp.value());
//                t.setVariableOpt("param.type", (dv != null) ? dv.value() : "");
//                t.setVariableOpt("param.description", (comment!=null) ? comment : "" );
//                t.addBlock("parameters");
//                info("add query param [%s] default [%s]", paramElement.getSimpleName(), dv);
//            } else {
//                FormParam fp = paramElement.getAnnotation(FormParam.class);
//                if (fp != null) {
//                    t.setVariableOpt("param.name", fp.value());
//                    t.setVariableOpt("param.type", (dv != null) ? dv.value() : "");
//                    t.setVariableOpt("param.description", (comment!=null) ? comment : "" );
//                    t.addBlock("parameters");
//                    info("add form param [%s] default [%s]", paramElement.getSimpleName(), dv);
//                }
// donot want params to be taken from tag Durga
// else {
//                    if( tag!=null ) {
//                        t.setVariableOpt("param.name", tag.getParameters()[0]);
//                        t.setVariableOpt("param.default", (dv != null) ? dv.value() : "");
//                        t.setVariableOpt("param.description", tag.getValue() );
//                        t.addBlock("parameters");
//                        info("add param [%s] description [%s]", paramElement.getSimpleName(), tag.getValue());
//                    }
//               }
//            }

        //   }

    }

    /**
     *
     * <!-- $BeginBlock services -->| *Description:* | ${service.description} |
     * | *Since:* | ${service.version} | | *Notes:* | ${service.notes} | |
     * *Security:* | ${service.security} | | *Usage:* | ${service.verb}
     * ${service.path} | | *Consumes* | ${service.consumes} | | *Produces:* |
     * ${service.produces} |
     *
     *
     * @param theClass
     * @param ee
     */
    private void processService(MiniTemplator t, ExecutableElement ee)  {

        Element enclosingEement = ee.getEnclosingElement();

        javax.ws.rs.Path path = enclosingEement.getAnnotation(javax.ws.rs.Path.class);

        javax.ws.rs.Path subPath = ee.getAnnotation(javax.ws.rs.Path.class);


        {
            Deprecated deprecated = ee.getAnnotation(Deprecated.class);
            if (deprecated != null) {
                t.setVariable(SERVICE_NOTES_VAR, "DEPRECATED", true);
            } else {
                t.setVariable(SERVICE_NOTES_VAR, "", true);
            }
        }

        {
            Object verb;

            verb = ee.getAnnotation(javax.ws.rs.GET.class);
            if (verb != null) {
                t.setVariable(SERVICE_VERB_VAR, "GET", false);
            } else {
                verb = ee.getAnnotation(javax.ws.rs.POST.class);
                if (verb != null) {
                    t.setVariable(SERVICE_VERB_VAR, "POST", false);
                } else {

                    verb = ee.getAnnotation(javax.ws.rs.PUT.class);
                    if (verb != null) {
                        t.setVariable(SERVICE_VERB_VAR, "PUT", false);
                    } else {

                        verb = ee.getAnnotation(javax.ws.rs.DELETE.class);
                        if (verb != null) {
                            t.setVariable(SERVICE_VERB_VAR, "DELETE", false);
                        }

                    }
                }
            }
        }

        {

            Produces produces = ee.getAnnotation(Produces.class);

            if (produces != null) {

                String value[] = produces.value();

                t.setVariableOpt(SERVICE_PRODUCES_VAR, escape(Arrays.asList(value).toString()));

            } else {
                t.setVariableOpt(SERVICE_PRODUCES_VAR, "N/A");
            }

        }


        {

            Consumes consumes = ee.getAnnotation(Consumes.class);

            if (consumes != null) {

                String value[] = consumes.value();

                t.setVariableOpt(SERVICE_CONSUMES_VAR, escape(Arrays.asList(value).toString()));

            } else {
                t.setVariableOpt(SERVICE_CONSUMES_VAR, "N/A");
            }
        }

        {
            StringBuilder sb = new StringBuilder();
            sb.append(path.value());
            if (subPath != null) {
                //if we add / here it shows as //
                sb.append(subPath.value());
            }
            //replacing { with HTML readable by confluence otherwise confluence is considering it as macros and giving error
            String servicePath = sb.toString().replace("{", "&#123;").replace("}", "&#125;");
            t.setVariable(SERVICE_PATH_VAR, escape(servicePath), false);

            java.util.Map<String, String> vars = t.getVariables();


            info("service [%s] verb [%s] path [%s] consumes [%s] produces [%s]",
                    vars.get(SERVICE_NAME_VAR),
                    vars.get(SERVICE_VERB_VAR),
                    vars.get(SERVICE_PATH_VAR),
                    vars.get(SERVICE_CONSUMES_VAR),
                    vars.get(SERVICE_PRODUCES_VAR));
        }


        try {
            processDocletForElement(t, enclosingEement, ee);

        } catch (IOException ex) {
            warn("error processing doclet", ex);
        }

    }

    private void populateParamDefault(Type eachJavaParameterType, MiniTemplator t, String variableOptName) {
        if (eachJavaParameterType.isPrimitive() || (eachJavaParameterType.getValue().equalsIgnoreCase("java.lang.String"))) {
            t.setVariableOpt(variableOptName, eachJavaParameterType.getGenericValue());
        } else {
            String subparams = "";
            JavaClass javaClass = eachJavaParameterType.getJavaClass();
            for (int j = 0; j < javaClass.getBeanProperties().length; j++) {
                BeanProperty[] beanProperties = javaClass.getBeanProperties();
                subparams = subparams + beanProperties[j].getName() + " : " + beanProperties[j].getType() + "\n";
            }
            if (subparams != "") {
                t.setVariableOpt(variableOptName, subparams);
            } else {
                t.setVariableOpt(variableOptName, eachJavaParameterType.getGenericValue());
            }
        }
    }

    private void populateExceptions(Type[] exceptionsType, MiniTemplator t, JavaMethod method) {
        if (exceptionsType.length == 0) {
            t.setVariableOpt(SERVICE_EXCEPTIONS_VAR, getTagByName(method, "exception", ""));
        } else {
            String exceptionsTypes = "";
            for (int i = 0; i < exceptionsType.length; i++) {
                exceptionsTypes = exceptionsTypes + exceptionsType[i].getValue() + "\n";
            }
            t.setVariableOpt(SERVICE_EXCEPTIONS_VAR, exceptionsTypes);
        }
    }

    private void populateSeeTag(MiniTemplator t, JavaMethod method) {
        if (getTagByName(method, "see", "") != "") {
            t.setVariableOpt(SERVICE_SEE_VAR, getTagByName(method, "see", ""));
        } else {
            t.setVariableOpt(SERVICE_SEE_VAR, "N/A");
        }
    }

    private void populateInputParameters(MiniTemplator t, JavaMethod method) {
        JavaParameter[] javaParameters = method.getParameters();
        if (!(javaParameters.length == 0)) {
            for (int i = 0; i < javaParameters.length; i++) {
                JavaParameter eachJavaParameter = javaParameters[i];

                Annotation[] annotations = eachJavaParameter.getAnnotations();
                if (annotations.length > 0) {
                    String annotationType = annotations[0].getType().toString();
                    t.setVariableOpt(SERVICE_PARAMNAME_VAR, eachJavaParameter.getName() + " (" + annotationType + ")");
                } else {
                    t.setVariableOpt(SERVICE_PARAMNAME_VAR, eachJavaParameter.getName());
                }

                t.setVariableOpt(SERVICE_PARAMTYPE_VAR, eachJavaParameter.getType().getValue());
                populateParamDefault(eachJavaParameter.getType(), t, SERVICE_PARAMDEFAULT_VAR);
                t.addBlock("parameters");
            }
        } else {
            t.setVariableOpt(SERVICE_PARAMNAME_VAR, "N/A");
            t.setVariableOpt(SERVICE_PARAMTYPE_VAR, "N/A");
            t.setVariableOpt(SERVICE_PARAMDEFAULT_VAR, "N/A");
            t.addBlock("parameters");
        }
    }


}