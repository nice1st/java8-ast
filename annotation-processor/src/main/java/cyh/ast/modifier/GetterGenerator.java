package cyh.ast.modifier;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

public class GetterGenerator {

	private final TreeMaker treeMaker;
	private final Names names;

	public GetterGenerator(ASTModifier astModifier) {
		this.treeMaker = astModifier.getTreeMaker();
		this.names = astModifier.getNames();
	}

	public JCTree.JCMethodDecl createGetterTree(JCTree.JCVariableDecl member) {
		String name = member.name.toString();
		char chars[] = name.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		String getterName = "get".concat(new String(chars));

		return treeMaker.MethodDef(
		  treeMaker.Modifiers(1),
		  names.fromString(getterName),
		  (JCTree.JCExpression)member.getType(),
		  List.nil(),
		  List.nil(),
		  List.nil(),
		  treeMaker.Block(1,
			List.of(treeMaker.Return(treeMaker.Ident(member.getName())))
		  ),
		  null
		);
	}
}
