/* Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution */
package st.redline;

import st.redline.bootstrap.AccessClassMethod;
import st.redline.bootstrap.AtSelectorPutMethod;
import st.redline.bootstrap.CreateSubclassMethod;

import java.io.File;
import java.util.Map;

public class Bootstrapper {

	PrimObjectMetaclass primObjectMetaclass;

	Bootstrapper(PrimObjectMetaclass primObjectMetaclass) {
		this.primObjectMetaclass = primObjectMetaclass;
	}

	public void bootstrap() {
		markBootstrapping(true);
		mapPackages(PrimObjectMetaclass.IMPORTS);
		createAndRegisterProtoObject();
		markBootstrapping(false);
	}

	void markBootstrapping(boolean bootstrapping) {
		PrimObject.BOOTSTRAPPING = bootstrapping;
	}

	void createAndRegisterProtoObject() {
		PrimObjectMetaclass protoObjectMetaClass = PrimObjectMetaclass.basicSubclassOf(primObjectMetaclass);
		protoObjectMetaClass.methods().put("<", new CreateSubclassMethod());
		protoObjectMetaClass.methods().put("atSelector:put:", new AtSelectorPutMethod());
		protoObjectMetaClass.methods().put("class", new AccessClassMethod());
		PrimObjectMetaclass protoObjectClass = protoObjectMetaClass.basicCreate("ProtoObject", null, "", "", "", "");
		PrimObject.CLASSES.put("st.redline.ProtoObject", protoObjectClass);
	}

	void mapPackages(Map<String, String> imports) {
		imports.put("ProtoObject", "st.redline.ProtoObject");
		for (String sourceFile : SourceFileFinder.findIn("st" + File.separator + "redline")) {
			String packageName = ClassPathUtilities.filenameWithExtensionToPackageName(sourceFile);
			String name = ClassPathUtilities.filenameToClassName(sourceFile);
			imports.put(name, packageName + "." + name);
		}
	}
}
