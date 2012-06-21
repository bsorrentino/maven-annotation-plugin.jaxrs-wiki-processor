/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bsc.jersey;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import biz.source_code.miniTemplator.MiniTemplator;
import biz.source_code.miniTemplator.MiniTemplator.BlockNotDefinedException;
import biz.source_code.miniTemplator.MiniTemplator.VariableNotDefinedException;

/**
 *
 * @author softphone
 */
@SupportedSourceVersion(SourceVersion.RELEASE_5)
@SupportedAnnotationTypes("javax.ws.rs.Path")
public class JerseyWikiProcessor extends AbstractProcessor {

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
    }

    protected void error( String msg ) {
        //logger.severe(msg);
        processingEnv.getMessager().printMessage(Kind.ERROR, msg );
    }

    protected void error( String msg, Throwable t ) {
        //logger.log(Level.SEVERE, msg, t );
        processingEnv.getMessager().printMessage(Kind.ERROR, msg );
    }

    @Override
    public boolean process(@SuppressWarnings("rawtypes") Set annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver())      return false;

        Filer filer = processingEnv.getFiler();

        final String resource = "widget-settings.xml";
        final String packageName = "";
        try {

        	/*
            FileObject f = filer.getResource(StandardLocation.CLASS_PATH, packageName, resource);

            //java.io.Reader r = f.openReader(true);  // ignoreEncodingErrors 
            java.io.InputStream is = f.openInputStream();

            if( is==null ) {
                warn( String.format("resource [%s] not found!", resource) );
                return false;
            }
            */
            java.net.URL template = getClass().getClassLoader().getResource("ScriptWikiTemplate.txt");

            if( template==null ) {
                error( "no template found!");
                return false;
            }
    
            Class<?> clazz = Class.forName("it.softphone.gpws.cme.bean.CMEBean");
            
            Method[] methods = clazz.getDeclaredMethods();
            
            MiniTemplator t = new MiniTemplator( template );

            t.setVariable("object", "CME");
            
            for( Method m : methods ) {
            	
            	int mod = m.getModifiers();
            	
            	if( !Modifier.isPublic( mod ) ) continue;
            	if( Modifier.isAbstract( mod ) ) continue;
            	
            	Deprecated d = m.getAnnotation(Deprecated.class);
            	
            	Class<?> ret = m.getReturnType();	
            	String name = m.getName();
            	String params = "";
            	String desc = (d==null) ? "" : "deprecated";
                try {
                    t.setVariable("name", name);
                    t.setVariable("result", ret.getSimpleName() );
                    t.setVariable("params", params);
                    t.setVariable("description", desc);

                    t.addBlock("methods");


                } catch (BlockNotDefinedException ex) {
                    error( "block setters not defined!");
                } catch (VariableNotDefinedException ex) {
                    error( "variable not set!", ex );
                }

            }

            FileObject res = filer.createResource(StandardLocation.SOURCE_OUTPUT, "confluence", "children/object-definition.wiki", (javax.lang.model.element.Element)null);
            
            java.io.Writer w = res.openWriter();
            
            t.generateOutput(w);
            
            w.close();
            
/*            
            Settings settings = Settings.create( is );

            java.util.Collection<Settings.Widget> widgets = settings.widgets();

            info( String.format("# of widget [%d]", widgets.size()) );

            MiniTemplator t = new MiniTemplator(new java.io.InputStreamReader(template) );

            for( Settings.Widget w : widgets ) {

                generateWidget( settings, t, w, filer );
            }
*/
        } catch (Exception ex) {
            error( "error on processing", ex);
            return false;
        }

        return true;
    }

/*    
    private Class<?> typeName( Class<?> type ) {
        if( Boolean.TYPE.isAssignableFrom(type) ) return Boolean.class;
        if( Integer.TYPE.isAssignableFrom(type) ) return Integer.class;
        if( Float.TYPE.isAssignableFrom(type) ) return Float.class;
        if( Double.TYPE.isAssignableFrom(type) ) return Double.class;

        return type;
    }

    private boolean acceptable( Class<?> type ) {
        if( type == null ) return false;

        return (
                Boolean.TYPE.isAssignableFrom(type) ||
                Boolean.class.isAssignableFrom(type) ||
                Integer.TYPE.isAssignableFrom(type) ||
                Integer.class.isAssignableFrom(type) ||
                Float.TYPE.isAssignableFrom(type) ||
                Float.class.isAssignableFrom(type) ||
                Double.TYPE.isAssignableFrom(type) ||
                Double.class.isAssignableFrom(type) ||
                //java.util.List.class.isAssignableFrom(type) ||
                 String.class.isAssignableFrom(type)
                );
    }

    private String getClassNameWithoutGeneric(  Widget widget ) {
        String target = widget.getName();
        int index = target.indexOf('<');
        if( index > 0 ) {
            target = target.substring(0, index);
        }
        return target;
}

    private String getTargetClassName( Widget widget ) {
            String target = widget.getTarget();
            int index = target.indexOf('<');
            if( index > 0 ) {
                target = target.substring(0, index);
            }
            return target;
    }
    
    private String getTargetConcreteClassName( Widget widget ) {
            String target = widget.getTarget();
            int index = target.indexOf('<');
            if( index > 0 ) {
                String generic = target.substring(index);
                if( generic.indexOf('?') != -1 )
                    target = target.substring(0, index);
            }
            return target;
    }

    private void generateFields( MiniTemplator t, Widget widget )  {
        try {
            
            //Class<?> c = Class.forName(widget.getTarget(), false, Thread.currentThread().getContextClassLoader());
            Class<?> c = Class.forName(getTargetClassName(widget), false, getClass().getClassLoader());

            BeanInfo bi = java.beans.Introspector.getBeanInfo(c);
            
            PropertyDescriptor[] pds = bi.getPropertyDescriptors();
            
            for (PropertyDescriptor pd : pds) {

                Class<?> type = pd.getPropertyType();

                //Method getter = pd.getReadMethod();

                Method setter = pd.getWriteMethod();

                if ( setter != null && acceptable(type) ) {

                    try {
                        t.setVariable("setter", setter.getName());
                        t.setVariable("type", type.getName());
                        t.setVariable("typeName", typeName(type).getSimpleName());
                        t.setVariable("propertyName", pd.getName());

                        t.addBlock("setters");


                    } catch (BlockNotDefinedException ex) {
                        error( "block setters not defined!");
                    } catch (VariableNotDefinedException ex) {
                        error( "variable not set!", ex );
                    }

                    try {
                        t.setVariable("type", typeName(type).getName());
                        t.setVariable("typeName", typeName(type).getSimpleName());
                        t.setVariable("setter", setter.getName());
                        t.setVariable("propertyName", pd.getName());

                        t.addBlock("init");


                    } catch (BlockNotDefinedException ex) {
                        error( "block setters not defined!");
                    } catch (VariableNotDefinedException ex) {
                        error( "variable not set!", ex );
                    }
                }
            }
        } catch (IntrospectionException ex) {
            error( "error reading bean info", ex);
        } catch (ClassNotFoundException ex) {
            error( "class not found!", ex);
        }

    }
    
    private void generateWidget(
            Settings settings,
            MiniTemplator t,
            Widget widget,
            Filer filer ) throws TemplateSyntaxException, IOException, VariableNotDefinedException, ClassNotFoundException, IntrospectionException, BlockNotDefinedException {


        String javaSource = String.format( "%s.%s.%s", settings.getPackage(), widget.getSubpackage(), getClassNameWithoutGeneric(widget) );


        info( javaSource );

        JavaFileObject source = filer.createSourceFile( javaSource );

        t.reset();
        //String date = dateFmt.format(new java.util.Date());

        t.setVariable("package", String.format( "%s.%s", settings.getPackage(), widget.getSubpackage()) );
        t.setVariable("class", widget.getName());
        t.setVariable("superclass", widget.getExtends());
        t.setVariable("target", widget.getTarget());
        t.setVariable("targetClass", getTargetConcreteClassName(widget));
        t.setVariable("body", widget.getcustomBody(), true);
        t.setVariable("init", widget.getCustomInit(), true);

        generateFields(t, widget);

        java.io.Writer w = source.openWriter();
        t.generateOutput(w);
        w.close();

    }
*/
}
