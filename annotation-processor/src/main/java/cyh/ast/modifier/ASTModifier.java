package cyh.ast.modifier;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import lombok.Getter;

public class ASTModifier {

	@Getter
	private final Trees trees;
	@Getter
	private final TreeMaker treeMaker;
	@Getter
	private final Names names;
	private TreePathScanner<Object, CompilationUnitTree> scanner;

	public ASTModifier(ProcessingEnvironment processingEnvironment) {
		Trees trees;
		try {
			trees = Trees.instance(processingEnvironment);
		} catch (IllegalArgumentException exception) {
			processingEnvironment.getMessager().printMessage(
			  Diagnostic.Kind.WARNING, "IllegalArgumentException on Intellij !!! -> do unwrap");
			processingEnvironment = jbUnwrap(ProcessingEnvironment.class, processingEnvironment);
			trees = Trees.instance(processingEnvironment);
		}
		this.trees = trees;

		JavacProcessingEnvironment javacProcessingEnvironment = (JavacProcessingEnvironment) processingEnvironment;
		Context context = javacProcessingEnvironment.getContext();
		this.treeMaker = TreeMaker.instance(context);
		this.names = Names.instance(context);
	}

	// for IntelliJ
	private static <T> T jbUnwrap(Class<? extends T> clazz, T wrapper) {
		T unwrapped = null;
		try {
			final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
			final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
			unwrapped = clazz.cast(unwrapMethod.invoke(null, clazz, wrapper));
		} catch (Throwable ignored) {}
		return unwrapped != null ? unwrapped : wrapper;
	}

	public void setClassDefModifyStrategy(BiConsumer<JCTree.JCClassDecl, EndPosTable> strategy) {
		this.scanner = new TreePathScanner<Object, CompilationUnitTree>() {
			@Override
			public Trees visitClass(ClassTree node, CompilationUnitTree compilationUnitTree) {
				JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) compilationUnitTree;
				EndPosTable endPosTable = compilationUnit.endPositions;
				if (compilationUnit.sourcefile.getKind() == JavaFileObject.Kind.SOURCE) {
					compilationUnit.accept(new TreeTranslator() {
						@Override
						public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
							super.visitClassDef(jcClassDecl);
							strategy.accept(jcClassDecl, endPosTable);
						}
					});
				}

				return trees;
			}
		};
	}

	public void modifyTree(Element element) {
		TreePath path = trees.getPath(element);
		scanner.scan(path, path.getCompilationUnit());
	}

}
