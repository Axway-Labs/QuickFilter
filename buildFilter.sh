#!/bin/sh
export IMPCP=build/jar/QuickFilter.jar

for i in ${VORDEL_HOME}/system/lib/plugins/*.jar; do
	export IMPCP=${IMPCP}:$i
done

for i in ${VORDEL_HOME}/system/lib/modules/*.jar; do
	export IMPCP=${IMPCP}:$i
done

for i in ${VORDEL_HOME}/system/lib/modules/xalan-j/*.jar; do
	export IMPCP=${IMPCP}:$i
done

for i in ${VORDEL_HOME}/system/lib/*.jar; do
	export IMPCP=${IMPCP}:$i
done

java -Dpython.import.site=false -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl -cp ${IMPCP} com.vordel.circuit.ext.filter.quick.QuickScriptFilterBuilder "$1"


