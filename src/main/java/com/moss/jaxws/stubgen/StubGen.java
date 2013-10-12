/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of jaxws-stubgen.
 *
 * jaxws-stubgen is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * jaxws-stubgen is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jaxws-stubgen; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.jaxws.stubgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StubGen {
	
	public static File[] generate(Class<?> serviceEndpoint, File dest) {
		return new StubGen().gen(serviceEndpoint, dest);
	}

	private File[] gen(Class<?> serviceEndpoint, File dest) {
		
		if (!dest.exists() && !dest.mkdirs()) {
			throw new RuntimeException("Directory does not exist and could not be created:" + dest.getAbsolutePath());
		}
		
		String packageName = serviceEndpoint.getPackage().getName() + ".jaxws";

		Method[] methods = serviceEndpoint.getMethods();
		
		List<File> generatedFiles = new ArrayList<File>();
		
		for (Method method : methods) {
			
			File request = generateRequest(method, packageName, dest);
			generatedFiles.add(request);
			
			boolean oneWay = false;
			{
				for (Annotation annotation : method.getAnnotations()) {
					String name = annotation.annotationType().getName();
					if (name.equals("javax.jws.Oneway")) {
						oneWay = true;
						break;
					}
				}
			}
			
			if (oneWay) {
//				System.out.println("@Oneway found on method " + method + ", not generating response/exception wrappers: " + serviceEndpoint);
			}
			else {
//				System.out.println("@Oneway NOT found on method " + method + ", generating response/exception wrappers: " + serviceEndpoint);
				
				File response = generateResponse(method, packageName, dest);
				List<File> exceptions = generateExceptions(method, packageName, dest);
			
				generatedFiles.add(response);
				generatedFiles.addAll(exceptions);
			}
		}
		
		return generatedFiles.toArray(new File[0]);
	}

	private File generateRequest(Method method, String packageName, File dest) {
		
		StringBuffer code = new StringBuffer();
		code.append("package " + packageName + ";\n");
		code.append("\n");
		
		// IMPORTS
		
		{
			Class<?>[] deps = getRequestDependencies(method, packageName);
			
			Arrays.sort(deps, new Comparator<Class<?>>() {
				public int compare(Class<?> o1, Class<?> o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			
			if (deps.length > 0) {
			
				for (Class<?> clazz : deps) {
					code.append("import " + clazz.getName() + ";\n");
				}	
				code.append("\n");
			}
		}
		
		code.append(generateDoNotEditBlurb());
		code.append("\n");

		// START CLASS
		code.append("public class " + initalCap(method.getName()) + " {\n\n");

		// FIELDS
		{
			int x=0;
			for (Type t : method.getGenericParameterTypes()) {
				
				String typeDecl = getTypeDeclaration(t);
				code.append("    private " + typeDecl + " arg" + x + ";\n");
				
				if (x + 1 < method.getGenericParameterTypes().length) {
					code.append("\n");
				}
				
				x++;
			}
		}
		code.append("\n");

		// METHODS
		{
			int x=0;
			for (Type t : method.getGenericParameterTypes()) {
				
				String typeDecl = getTypeDeclaration(t);
				makeGetter("arg" + x, typeDecl, code);
				
				code.append("\n");
				
				makeSetter("arg" + x, typeDecl, code);
				
				if (x + 1 < method.getGenericParameterTypes().length) {
					code.append("\n");
				}
				
				x++;
			}
		}

		// END CLASS
		code.append("}\n");

//		System.out.println(code);
		File file = new File(dest, initalCap(method.getName())+".java");
		
		write(code, file);
		
		return file;
	}

	private Class getRootClass(Type type){
		if(type instanceof Class) 
			return (Class)type;
		else if(type instanceof ParameterizedType)
			return getRootClass((ParameterizedType)type);
		
		return null;
	}
	private Class getRootClass(ParameterizedType type){
		Type raw = type.getRawType();
		if(raw instanceof Class)
			return (Class)raw;
		else if(raw instanceof ParameterizedType) 
			return getRootClass((ParameterizedType)raw);
		else
			throw new RuntimeException("Should never get here");
	}
	

	private File generateResponse(Method method, String packageName, File dest) {
		
		StringBuffer code = new StringBuffer();
		code.append("package " + packageName + ";\n");
		code.append("\n");
		
		// IMPORTS
		{
			Class<?>[] deps = getResponseDependencies(method, packageName);
			
			Arrays.sort(deps, new Comparator<Class>() {
				public int compare(Class o1, Class o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			
			if (deps.length > 0) {
			
				for (Class<?> clazz : deps) {
					code.append("import " + clazz.getName() + ";\n");
				}	
				code.append("\n");
			}
		}
		
		code.append(generateDoNotEditBlurb());
		code.append("\n");
		
		// START CLASS
		code.append("public class " + initalCap(method.getName()) + "Response {\n\n");

		String typeDecl = getTypeDeclaration(method.getGenericReturnType());
		
		if(!typeDecl.equals("void")) {
			
			// FIELDS
			code.append("    private " + typeDecl + " Return" + ";\n");
			code.append("\n");

			// METHODS
			makeGetter("Return", typeDecl, code);
			
			code.append("\n");
			
			makeSetter("Return", typeDecl, code);
		}

		// END CLASS
		code.append("}\n");
		
//		System.out.println(code);
		
		File file = new File(dest, initalCap(method.getName()) + "Response.java");

		write(code, file);
		
		return file;
	}
	
	private List<File> generateExceptions(Method method, String packageName, File dest) {
		
		List<File> files = new ArrayList<File>();
		
		for (Class exceptionType : method.getExceptionTypes()) {
			
			StringBuffer code = new StringBuffer();
			code.append("package " + packageName + ";\n");
			code.append("\n");
			
			// IMPORTS
			code.append("import javax.xml.bind.annotation.XmlRootElement;\n");
			code.append("\n");
			
			code.append(generateDoNotEditBlurb());
			code.append("\n");
			
			// START CLASS
			code.append("@XmlRootElement\n");
			code.append("public class " + exceptionType.getSimpleName() + "Bean {\n\n");

			// END CLASS
			code.append("}\n");
			
			File file = new File(dest, exceptionType.getSimpleName() + "Bean.java");
			
			write(code, file);
			
			files.add(file);
		}
		
		return files;
	}
	
	private String generateDoNotEditBlurb() {
		return new StringBuilder()
		.append("/**\n")
		.append(" * WARNING: This file was dynamically generated by jaxws-stubgen! Any manual\n")
		.append(" * changes made to this file will be overwritten.\n")
		.append(" */")
		.toString();
	}
	
	private Class<?>[] getRequestDependencies(Method method, String targetPackage) {
		
		List<Type> typesToExamine = new ArrayList<Type>();
		typesToExamine.addAll(Arrays.asList(method.getGenericParameterTypes()));
		
		return getTypeDependencies(targetPackage, typesToExamine.toArray(new Type[0]));
	}
	
	private Class<?>[] getResponseDependencies(Method method, String targetPackage) {
		
		List<Type> typesToExamine = new ArrayList<Type>();
		typesToExamine.add(method.getGenericReturnType());
		
		/*
		 * TODO: something should probably be done with this
		 */
//		typesToExamine.addAll(Arrays.asList(method.getGenericExceptionTypes()));
		
		return getTypeDependencies(targetPackage, typesToExamine.toArray(new Type[0]));
	}
	
	private Class<?>[] getTypeDependencies(String targetPackage, Type[] typesToExamine) {
		 
		Set<Class> referencedClasses = new HashSet<Class>();
		
		for (Type examinedType : typesToExamine) {
		
			if (examinedType instanceof ParameterizedType) {

				ParameterizedType pt = (ParameterizedType) examinedType;

				referencedClasses.add((Class<?>)pt.getRawType());

				for (Type t : pt.getActualTypeArguments()) {
					referencedClasses.add(getRootClass(t));
				}
			}
			else if (examinedType instanceof GenericArrayType) {
				referencedClasses.add( (Class<?>) ((GenericArrayType)examinedType).getGenericComponentType() );
			}
			else if ( ((Class<?>)examinedType).isArray() ) {
				referencedClasses.add( ((Class<?>)examinedType).getComponentType() );
			}
			else {
				referencedClasses.add((Class<?>)examinedType);
			}
		}
		
		List<Class<?>> required = new ArrayList<Class<?>>();
		
		for (Type t : referencedClasses) {
			
			if(t instanceof Class){
				Class<?> c = (Class<?>)t;
				
				if (c == Void.class || c.isPrimitive()) {
					continue;
				}
				
				String referencedPackage = c.getPackage().getName();
				
				if (targetPackage.equals(referencedPackage)) {
					continue;
				}
				
				required.add(c);
			}
		}
		
		return required.toArray(new Class<?>[0]);
	}
	
	private String getTypeDeclaration(Type type) {

		Class<?> classType;
		List<Class<?>> typeParameters = new ArrayList<Class<?>>();
		boolean isArray = false;

		if (type instanceof ParameterizedType) {

			ParameterizedType pt = (ParameterizedType) type;

			classType = (Class<?>)pt.getRawType();

			for (Type t : pt.getActualTypeArguments()) {
				typeParameters.add(getRootClass(t));
			}
		}
		else if (type instanceof GenericArrayType) {
			classType = (Class<?>) ((GenericArrayType)type).getGenericComponentType();
			isArray = true;
		}
		else if ( ((Class<?>)type).isArray() ) {
			classType = ((Class<?>)type).getComponentType();
			isArray = true;
		}
		else {
			classType = (Class<?>)type;
		}
		
		StringBuilder decl = new StringBuilder();
		
		decl.append(classType.getSimpleName());
		
		if (!typeParameters.isEmpty()) {
			decl.append("<");
			
			int count = 0;
			for (Class<?> param : typeParameters) {
				
				decl.append(param.getSimpleName());
			
				if (count + 1 < typeParameters.size()) {
					decl.append(", ");
				}
				
				count++;
			}
			
			decl.append(">");
		}
		
		if (isArray) {
			decl.append("[]");
		}
		
		return decl.toString();
	}

	
	private void write(StringBuffer code, File path){
		try {
//			System.out.println(path);
			FileWriter writer = new FileWriter(path);
			writer.write(code.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void makeGetter(String propertyName, String declarationType, StringBuffer code){
		code.append("    public " + declarationType + " get" + initalCap(propertyName) + "() {\n");
		code.append("        return " + propertyName + ";\n");
		code.append("    }\n");

	}
	private void makeSetter(String propertyName, String declarationType, StringBuffer code){
		code.append("    public void set" + initalCap(propertyName) + "(" + declarationType + " " + propertyName + ") {\n");
		code.append("        this." + propertyName + " = " + propertyName +  ";\n");
		code.append("    }\n");

	}

	private String initalCap(String text){
		return text.substring(0, 1).toUpperCase() + text.substring(1);
	}
}
