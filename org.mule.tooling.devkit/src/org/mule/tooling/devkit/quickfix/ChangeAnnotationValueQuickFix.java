package org.mule.tooling.devkit.quickfix;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.swt.graphics.Image;
import org.mule.tooling.devkit.ASTUtils;
import org.mule.tooling.devkit.common.DevkitUtils;

@SuppressWarnings("restriction")
public class ChangeAnnotationValueQuickFix extends QuickFix {

	private final String annotation;

	public ChangeAnnotationValueQuickFix(String label,
			ConditionMarkerEvaluator evaluator) {
		super(label, evaluator);
		this.annotation = "Default";
	}

	public String getLabel() {
		return label;
	}

	protected void createAST(ICompilationUnit unit, Integer charStart)
			throws JavaModelException {

		CompilationUnit parse = ASTUtils.parse(unit);
		LocateAnnotationVisitor visitor = new LocateAnnotationVisitor(
				charStart, annotation);

		LocateFieldOrMethodVisitor visitorField = new LocateFieldOrMethodVisitor(
				charStart);

		parse.accept(visitor);

		if (visitor.getNode() != null) {
			parse.accept(visitorField);
			AST ast = parse.getAST();

			ASTRewrite rewrite = ASTRewrite.create(ast);
			Annotation annotation = (Annotation) visitor.getNode();

			// TODO get first element in enum
			SimpleType type = (SimpleType) ((FieldDeclaration) visitorField
					.getNode()).getType();

			StringLiteral literal = ast.newStringLiteral();
			String value = getFirstItemOrDefault(unit, type);
			literal.setLiteralValue(value);

			rewrite.replace(
					(ASTNode) ((SingleMemberAnnotation) annotation).getValue(),
					literal, null);

			applyChange(unit, rewrite);
		}
	}

	private String getFirstItemOrDefault(ICompilationUnit unit, SimpleType type) {
		String value = "${value}";
		CompilationUnit clazz = getEnumClass(unit, type.getName().toString());
		if (clazz != null) {
			for (Object element : clazz.types()) {
				if (element instanceof EnumDeclaration) {
					EnumDeclaration enumDec = (EnumDeclaration) element;
					if (!enumDec.enumConstants().isEmpty()) {
						value = enumDec.enumConstants().get(0).toString();
						break;
					}
				}
			}
		}
		return value;
	}

	@Override
	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
	}

	private CompilationUnit getEnumClass(ICompilationUnit unit, String clazzName) {
		IProject project = unit.getJavaProject().getProject();
		IFolder folder = project.getFolder(DevkitUtils.MAIN_JAVA_FOLDER);

		return locateConnectorInResource(project,
				folder.getProjectRelativePath(), clazzName);
	}

	CompilationUnit locateConnectorInResource(IProject project,
			IPath folderResource, String clazzName) {
		IFolder folder = project.getFolder(folderResource.makeRelative());

		try {
			for (IResource resource : folder.members()) {
				IJavaElement element = (IJavaElement) resource
						.getAdapter(IJavaElement.class);
				if (element != null) {
					switch (element.getElementType()) {
					case IJavaElement.PACKAGE_FRAGMENT_ROOT:
						System.out.println(element);
						break;
					case IJavaElement.PACKAGE_FRAGMENT:
						System.out.println(element);
						return locateConnectorInResource(project, element
								.getPath().makeRelativeTo(folderResource),
								clazzName);
					case IJavaElement.COMPILATION_UNIT:

						if (((ICompilationUnit) element).getElementName()
								.equals(clazzName + ".java")) {
							CompilationUnit clazz = ASTUtils
									.parse((ICompilationUnit) element);
							return clazz;
						}
						break;
					default:
						System.out.println(element);
						break;
					}

				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}
}