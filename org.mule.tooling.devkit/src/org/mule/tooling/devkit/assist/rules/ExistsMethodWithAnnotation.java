package org.mule.tooling.devkit.assist.rules;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

public class ExistsMethodWithAnnotation extends ASTRule {

    List<String> name = new ArrayList<String>();

    public ExistsMethodWithAnnotation(String name) {
        super(0);
        this.name.add(name);
    }

    CompilationUnit cu;
    MethodDeclaration method;
    String annotation;

    public ExistsMethodWithAnnotation addAnnotation(String name) {
        this.name.add(name);
        return this;
    }

    public boolean visit(CompilationUnit node) {
        cu = node;
        return true;
    }

    public boolean visit(MethodDeclaration node) {
        method = node;
        if (applies) {
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        ASTNode parent = node.getParent();
        if (hasAnnotation(node) && parent.equals(method)) {
            applies = true;
        }
        return false;
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        ASTNode parent = node.getParent();
        if (hasAnnotation(node) && parent.equals(method)) {
            applies = true;
        }
        return false;
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {

        ASTNode parent = node.getParent();
        if (hasAnnotation(node) && parent.equals(method)) {
            applies = true;
        }
        return false;
    }

    private boolean hasAnnotation(Annotation node) {
        return name.contains(node.getTypeName().toString());
    }
}
