package org.mule.tooling.devkit.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.mule.tooling.devkit.ASTUtils;

public abstract class QuickFix implements IMarkerResolution2, DevkitQuickFix {
	String label;

	IResource resource;

	ConditionMarkerEvaluator evaluator;

	QuickFix(String label, ConditionMarkerEvaluator evaluator) {
		this.label = label;
		this.evaluator = evaluator;
	}
	

	public String getLabel() {
		return label;
	}

	public void run(IMarker marker) {

		try {
			resource = marker.getResource();
			IJavaElement javaElement = JavaCore.create(resource);
			ICompilationUnit cu = (ICompilationUnit) javaElement
					.getAdapter(ICompilationUnit.class);
			Integer charStart = (Integer) marker
					.getAttribute(IMarker.CHAR_START);
			createAST(cu, charStart);

			marker.delete();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void createAST(ICompilationUnit unit, Integer charStart)
			throws JavaModelException {
		CompilationUnit parse = ASTUtils.parse(unit);
		LocateAnnotationVisitor visitor = new LocateAnnotationVisitor(
				charStart, "Optional");

		parse.accept(visitor);

		if (visitor.getNode() != null) {
			ASTRewrite rewrite = ASTRewrite.create(parse.getAST());
			rewrite.remove(visitor.getNode(), null);
			applyChange(unit, rewrite);
		}
	}


	protected void applyChange(ICompilationUnit unit, ASTRewrite rewrite)
			throws JavaModelException {
		unit.applyTextEdit(rewrite.rewriteAST(), null);
		unit.becomeWorkingCopy(null);
		unit.commitWorkingCopy(true, null);
		unit.discardWorkingCopy();
	}

	protected void addImportIfRequired(CompilationUnit parse, AST ast,
			ASTRewrite rewrite, String fullyQualifiedName) {

		boolean hasConnectorAnnotationImport = false;

		ListRewrite listImports = rewrite.getListRewrite(parse,
				CompilationUnit.IMPORTS_PROPERTY);

		for (Object obj : parse.imports()) {
			ImportDeclaration importDec = (ImportDeclaration) obj;
			if (importDec.getName().getFullyQualifiedName()
					.equals(fullyQualifiedName)) {
				hasConnectorAnnotationImport = true;
			}
		}

		ImportDeclaration id = null;

		if (!hasConnectorAnnotationImport) {
			id = ast.newImportDeclaration();
			id.setName(ast.newName(fullyQualifiedName));
			listImports.insertLast(id, null);
		}
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_TOOL_DELETE);
	}

	public boolean hasFixForMarker(IMarker marker) {
		return evaluator.hasFixForMarker(marker);
	}
}