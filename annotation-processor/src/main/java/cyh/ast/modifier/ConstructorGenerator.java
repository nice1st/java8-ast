package cyh.ast.modifier;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class ConstructorGenerator {

	private final Types typeUtils;
	private final Trees trees;
	private final TreeMaker treeMaker;
	private final Names names;

	public ConstructorGenerator(Types typeUtils, ASTModifier astModifier) {
		this.typeUtils = typeUtils;
		this.trees = astModifier.getTrees();
		this.treeMaker = astModifier.getTreeMaker();
		this.names = astModifier.getNames();
	}

	public JCTree.JCMethodDecl copy(ExecutableElement superConstructor) {
		List<JCTree.JCVariableDecl> params = genParams(superConstructor);
		JCTree.JCExpressionStatement superCall = genSuperCall(params);

		JCTree.JCBlock body = treeMaker.Block(0, List.of(superCall));

		return treeMaker.MethodDef(
		  treeMaker.Modifiers(Flags.PUBLIC), // 접근 제어자
		  names.init, // 이름
		  treeMaker.TypeIdent(TypeTag.VOID), // 반환 타입
		  List.nil(), // 제네릭 파라미터
		  List.from(params), // 인자
		  List.nil(), // 예외
		  body, // 본문
		  null // 기본값
		);
	}

	private List<JCTree.JCVariableDecl> genParams(ExecutableElement constructor) {
		JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) trees.getTree(constructor);

		List<JCTree.JCVariableDecl> params = List.nil();
		for (JCTree.JCVariableDecl param : methodDecl.getParameters()) {
			params = params.append(treeMaker.Param(
			  param.getName(),
			  param.sym.type,
			  null
			));
		}
		return params;
	}

	private JCTree.JCExpressionStatement genSuperCall(List<JCTree.JCVariableDecl> params) {
		List<JCTree.JCExpression> args = List.nil();
		for (JCTree.JCVariableDecl param : params) {
			args = args.append(treeMaker.Ident(param.name));
		}

		return treeMaker.Exec( // 실행
		  treeMaker.Apply( // 메소드
			List.nil(), // 제네릭 파라미터
			treeMaker.Ident(names.fromString("super")), // 이름
			args // 인자
		  )
		);
	}

	public JCTree.JCMethodDecl dynamic(JCTree.JCMethodDecl superConstructor, TypeMirror interfaceType) {
		// 인터페이스 제너릭 파라메터: DynamicValue
		TypeMirror valueType = ((DeclaredType) interfaceType).getTypeArguments().get(0);
		// 배열 타입: DynamicValue[]
		TypeMirror arrayValueType = typeUtils.getArrayType(valueType);
		// Variable: DynamicValue[] _values
		JCTree.JCVariableDecl newParam = treeMaker.Param(names.fromString("_values"), (Type) arrayValueType, null);
		// DynamicValue[] -> DynamicValue...
		newParam.mods = treeMaker.Modifiers(Flags.PARAMETER | Flags.VARARGS);
		// process(_values);
		JCTree.JCExpressionStatement processCall = genProcessCall(newParam);

		/*
		생성자
		public constructor(..., DynamicValue[] _values) {
		  super(...);
		  process(_values);
		}
		 */
		return treeMaker.MethodDef(
		  treeMaker.Modifiers(Flags.PUBLIC), // 접근 제어자
		  names.init, // 이름
		  treeMaker.TypeIdent(TypeTag.VOID), // 반환 타입
		  List.nil(), // 제네릭 파라미터
		  superConstructor.params.append(newParam), // 인자
		  List.nil(), // 예외
		  treeMaker.Block(0, List.from(superConstructor.body.getStatements()).append(processCall)), // 본문
		  null // 기본값
		);
	}

	private JCTree.JCExpressionStatement genProcessCall(JCTree.JCVariableDecl newParam) {
		return treeMaker.Exec(
		  treeMaker.Apply(
			List.nil(),
			treeMaker.Ident(names.fromString("process")), // todo interface defs name
			List.of(treeMaker.Ident(newParam.name))
		  )
		);
	}

}
