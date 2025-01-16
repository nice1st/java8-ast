package cyh.ast.processor;

import java.util.List;
import java.util.Optional;
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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.tree.JCTree;

import cyh.ast.annotation.DynamicArgsConstructor;
import cyh.ast.modifier.ASTModifier;
import cyh.ast.modifier.ConstructorGenerator;

@AutoService(Processor.class)
@SupportedAnnotationTypes("cyh.ast.annotation.DynamicArgsConstructor")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DynamicArgsConstructorProcessor extends AbstractProcessor {

	private ASTModifier astModifier;
	private ConstructorGenerator constructorGenerator;
	private Elements elementUtils;
	private Types typeUtils;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "DynamicFieldProcessor init()");
		super.init(processingEnv);
		this.elementUtils = processingEnv.getElementUtils();
		this.typeUtils = processingEnv.getTypeUtils();
		this.astModifier = new ASTModifier(processingEnv);
		this.constructorGenerator = new ConstructorGenerator(typeUtils, astModifier);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "DynamicFieldProcessor process()");
		for (Element element : roundEnv.getElementsAnnotatedWith(DynamicArgsConstructor.class)) {
			if (element.getKind() != ElementKind.CLASS) {
				continue;
			}

			TypeElement classElement = (TypeElement) element;

			// 1. super class 여부 not Object
			TypeMirror superclass = classElement.getSuperclass();
			if (superclass.toString().equals(Object.class.getCanonicalName())) {
				processingEnv.getMessager().printMessage(
				  Diagnostic.Kind.WARNING,
				  String.format("DynamicArgsConstructor Generate Fail, because %s has not SuperClass.", classElement.getSimpleName()),
				  classElement
				);
				continue;
			}

			// 2. DynamicValueProcessor 구현체
			TypeElement dynamicValueProcessorElement = elementUtils.getTypeElement(DynamicValueProcessor.class.getCanonicalName());
			Optional<? extends TypeMirror> iDynamicValueProcessor = classElement.getInterfaces().stream()
			  .filter(typeMirror -> typeUtils.asElement(typeMirror).equals(dynamicValueProcessorElement))
			  .findAny();
			if (!iDynamicValueProcessor.isPresent()) {
				processingEnv.getMessager().printMessage(
				  Diagnostic.Kind.WARNING,
				  String.format("DynamicArgsConstructor Generate Fail, because %s has not implement DynamicValueProcessor.", classElement.getSimpleName()),
				  classElement
				);
				continue;
			}

			// 3. super 의 constructor 수집
			TypeElement superClassElement = (TypeElement) typeUtils.asElement(superclass);
			List<ExecutableElement> constructors = ElementFilter.constructorsIn(superClassElement.getEnclosedElements());

			// 4. constructor(..., value[] values) 생성 추가
			doProcess(classElement, constructors, iDynamicValueProcessor.get());
		}

		return true;
	}

	private void doProcess(TypeElement element, List<ExecutableElement> constructors, TypeMirror interfaceType) {
		try {
			astModifier.setClassDefModifyStrategy(jcClassDecl -> {
				for (ExecutableElement constructor : constructors) {
					if (constructor.getParameters().isEmpty()) {
						continue;
					}

					JCTree.JCMethodDecl copyConstructor = constructorGenerator.copy(constructor);
					JCTree.JCMethodDecl dynamicConstructor = constructorGenerator.executeDynamicProcessor(copyConstructor, interfaceType);
					jcClassDecl.defs = jcClassDecl.defs.append(copyConstructor).append(dynamicConstructor);
				}
			});

			astModifier.modifyTree(element);
			processingEnv.getMessager().printMessage(
			  Diagnostic.Kind.NOTE,
			  String.format("Generated %s DynamicArgsConstructors to %s", constructors.size(), element.getSimpleName()),
			  element
			);
		} catch (Exception e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error modifying class: " + e.getMessage());
		}
	}

	public interface DynamicValueProcessor<T> {

		void process(T[] values);
	}
}
