package cyh.ast.modifier;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class ASTModifier {

	private final JavacProcessingEnvironment javacProcessingEnvironment;
	private final Types typeUtils;
	private final Elements elementUtils;

	private final Trees trees;
	private final TreeMaker treeMaker;
	private final Names names;
	private TreePathScanner<Object, CompilationUnitTree> scanner;

	public ASTModifier(ProcessingEnvironment processingEnvironment, Types typeUtils, Elements elementUtils) {
		Trees trees;
		try {
			trees = Trees.instance(processingEnvironment);
		} catch (IllegalArgumentException exception) {
			processingEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING, "IllegalArgumentException!!! -> do unwrap");
			processingEnvironment = jbUnwrap(ProcessingEnvironment.class, processingEnvironment);
			trees = Trees.instance(processingEnvironment);
		}
		this.trees = trees;

		this.javacProcessingEnvironment = (JavacProcessingEnvironment) processingEnvironment;
		this.typeUtils = typeUtils;
		this.elementUtils = elementUtils;

		Context context = javacProcessingEnvironment.getContext();
		this.treeMaker = TreeMaker.instance(context);
		this.names = Names.instance(context);
	}

	// for IntelliJ
	private static <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
		T unwrapped = null;
		try {
			final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
			final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
			unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
		} catch (Throwable ignored) {}
		return unwrapped != null ? unwrapped : wrapper;
	}

	public void setClassDefModifyStrategy(Consumer<JCTree.JCClassDecl> strategy) {
		this.scanner = new TreePathScanner<Object, CompilationUnitTree>() {
			@Override
			public Trees visitClass(ClassTree node, CompilationUnitTree compilationUnitTree) {
				JCTree.JCCompilationUnit compilationUnit = (JCTree.JCCompilationUnit) compilationUnitTree;
				if (compilationUnit.sourcefile.getKind() == JavaFileObject.Kind.SOURCE) {
					compilationUnit.accept(new TreeTranslator() {
						@Override
						public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
							super.visitClassDef(jcClassDecl);
							strategy.accept(jcClassDecl);
						}
					});
				}

				return trees;
			}
		};
	}

	public void modifyTree(Element element) {
		TreePath path = trees.getPath(element);

		if (path == null) {
			throw new IllegalArgumentException("No TreePath found for the given element.");
		}
		scanner.scan(path, path.getCompilationUnit());
	}

	public JCTree.JCMethodDecl generateConstructor(ExecutableElement superConstructor, TypeMirror interfaceType) {
		// values parameter
		TypeMirror valueType = ((DeclaredType) interfaceType).getTypeArguments().get(0);
		TypeMirror arrayValueType = typeUtils.getArrayType(valueType);
		JCTree.JCVariableDecl newParam = treeMaker.Param(names.fromString("_values"), (Type) arrayValueType, null);

		JCTree.JCBlock body = treeMaker.Block(0, List.nil());
		// super(...);
		JCTree.JCExpressionStatement superCall = treeMaker.Exec(
		  treeMaker.Apply(
			List.nil(),
			treeMaker.Ident(names.fromString("super")),
			List.nil() // 부모 생성자 호출 시 인자가 없음을 가정
		  )
		);
		// process(values);
		body.stats = List.of(superCall); // 생성자 바디에 super() 추가

		return treeMaker.MethodDef(
		  treeMaker.Modifiers(Flags.PUBLIC), // 접근 제어자
		  names.init, // 생성자 이름
		  treeMaker.TypeIdent(TypeTag.VOID), // 반환 타입은 null (생성자는 반환 타입이 없음)
		  List.nil(), // 제네릭 파라미터
		  List.of(newParam),
		  List.nil(), // 예외
		  body, // 본문
		  null // 기본값
		);
	}
}
