package cyh.ast.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;

import cyh.ast.annotation.CustomGetter;
import cyh.ast.modifier.ASTModifier;
import cyh.ast.modifier.GetterGenerator;

@AutoService(Processor.class)
@SupportedAnnotationTypes("cyh.ast.annotation.CustomGetter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CustomGetterProcessor extends AbstractProcessor {

	private ASTModifier astModifier;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "CustomGetterProcessor init()");
		super.init(processingEnv);
		this.astModifier = new ASTModifier(processingEnv);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "CustomGetterProcessor process()");
		for (Element element : roundEnv.getElementsAnnotatedWith(CustomGetter.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				continue;
			}

			TypeElement classElement = (TypeElement) element;
			doProcess();
			astModifier.modifyTree(classElement);
		}

		return true;
	}

	private void doProcess() {
		astModifier.setClassDefModifyStrategy((jcClassDecl, endPosTable) -> {
			int pos = jcClassDecl.getEndPosition(endPosTable);
			astModifier.getTreeMaker().at(pos);

			GetterGenerator getterGenerator = new GetterGenerator(astModifier);
			List<JCTree> members = jcClassDecl.getMembers();
			for (JCTree member : members) {
				if (member instanceof JCTree.JCVariableDecl) {
					JCTree.JCMethodDecl getter = getterGenerator.createGetterTree((JCTree.JCVariableDecl) member);
					jcClassDecl.defs = jcClassDecl.defs.prepend(getter);
				}
			}
		});
	}

}
