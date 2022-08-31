package org;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;


public class Transformations {

    //todo if it doesnt have any constructor?
    public static boolean hasEmptyConstructor(TypeDeclaration node){

        for(MethodDeclaration method : node.getMethods()){
            if (method.isConstructor() && method.parameters().isEmpty()){
               return true;
            }
        }

        return false;
    }

    public static boolean hasGettersAndSetters(TypeDeclaration node, String variableName){

        String getMethodName = "get"+ variableName;
        String setMethodName = "set"+ variableName;
        for(MethodDeclaration method : node.getMethods()){
            String methodName = method.getName().toString().toLowerCase();
            if(methodName.contains(getMethodName.toLowerCase()) || methodName.contains(setMethodName.toLowerCase())){
                return true;
            }

        }

        return false;
    }

    public static void transform(final CompilationUnit cu, final boolean fullTransformation) {

        cu.accept(new ASTVisitor() {

            public boolean visit(FieldDeclaration node) {
                RemoveFinalModifierAndMakeFieldPublic(node);
                //addGettersAndSetters(node);
                //addgetset(node);
                return true;
            }

            public boolean visit(MethodDeclaration node) {
                RemoveFinalModifierAndMakeMethodPublic(node);
                return true;
            }
            public boolean visit(TypeDeclaration node){
                if (fullTransformation)
                    addEmptyConstructorAndMakeClassPublic(node, fullTransformation);
                addgettersAndSetters(node);
                changeClassAccessModifier(node);
                return true;
            }


        });
    }
    private static Block createGetterMethodBody(AST ast, String variableName)
    {
        String body = "return this."+ variableName+";";
        Block block = getBlockForSetterAndGetter(ast, body);

        return block;
    }
    private static Block createSetterMethodBody(AST ast, String variableName)
    {
        String body = "this."+ variableName+" = " + variableName + ";";
        Block block = getBlockForSetterAndGetter(ast, body);

        return block;
    }

    private static Block getBlockForSetterAndGetter(AST ast, String body) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setKind(ASTParser.K_STATEMENTS);
        parser.setSource(body.toCharArray());
        ASTNode astNodeWithMethodBody = parser.createAST(null);
        ASTNode convertedAstNodeWithMethodBody =
            ASTNode.copySubtree(ast, astNodeWithMethodBody);
        return (Block) convertedAstNodeWithMethodBody;
    }

    private static boolean hasFinalModifier(FieldDeclaration fieldNode){

        int i = 0;
        while (i < fieldNode.modifiers().size()) {
            if (fieldNode.modifiers().get(i) instanceof Modifier) {
                Modifier mod = (Modifier) fieldNode.modifiers().get(i);
                if (mod.isFinal()) {
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    public static void addgettersAndSetters(TypeDeclaration node){

        List<String> variableNamesGetter = new ArrayList<String>();
        List<Type> variableTypesGetter = new ArrayList<Type>();

        List<String> variableNamesSetter = new ArrayList<String>();
        List<Type> variableTypesSetter = new ArrayList<Type>();

        if(!node.isInterface()){
            for(FieldDeclaration field: node.getFields()){


                Object fragments = field.fragments().get(0);
                if(fragments instanceof VariableDeclarationFragment ){

                    String variableName = ((VariableDeclarationFragment) fragments).getName().toString();
                    if(!hasGettersAndSetters(node, variableName)) {
                        variableTypesGetter.add(field.getType());
                        variableNamesGetter.add(variableName);

                        if(!hasFinalModifier(field)){
                            variableTypesSetter.add(field.getType());
                            variableNamesSetter.add(variableName);
                        }

                    }
                }

            }
            ModifierKeyword publicMod = ModifierKeyword.PUBLIC_KEYWORD;
            AST astNode = node.getAST();


            //AST ast = node.getAST();
            for(int i = 0 ; i< variableNamesGetter.size(); i++) {
                Type type = variableTypesGetter.get(i);

                //add getter
                ASTNode converted = ASTNode.copySubtree(astNode, type);
                Type tipo = (Type) converted;

                MethodDeclaration getter = astNode.newMethodDeclaration();
                getter.setName(astNode.newSimpleName("get" + variableNamesGetter.get(i)));
                getter.modifiers().add(astNode.newModifier(publicMod));
                getter.setReturnType2(tipo);
                Block getterBody = createGetterMethodBody(astNode, variableNamesGetter.get(i));
                getter.setBody(getterBody);

                node.bodyDeclarations().add(getter);
            }

            for(int i = 0 ; i< variableNamesSetter.size(); i++) {
                Type type = variableTypesSetter.get(i);

                //add setter
                ASTNode converted2 = ASTNode.copySubtree(astNode,type);
                Type tipo2 = (Type) converted2;

                MethodDeclaration setter = astNode.newMethodDeclaration();
                setter.setName(astNode.newSimpleName("set"+ variableNamesSetter.get(i)));
                setter.modifiers().add(astNode.newModifier(publicMod));
                setter.setReturnType2(astNode.newPrimitiveType(PrimitiveType.VOID));
                SingleVariableDeclaration parameter = astNode.newSingleVariableDeclaration();
                parameter.setType(tipo2);
                parameter.setName(astNode.newSimpleName(variableNamesSetter.get(i)));
                setter.parameters().add(parameter);
                Block setterBody = createSetterMethodBody(astNode, variableNamesSetter.get(i));
                setter.setBody(setterBody);

                node.bodyDeclarations().add(setter);

            }

        }

    }

    public static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
    //loop directory to get file list
    public static void ParseFilesInDir() throws IOException{
        File dirs = new File(".");
        String dirPath = dirs.getCanonicalPath() + File.separator+"src"+File.separator;
        File root = new File(dirPath);
        File[] files = root.listFiles ( );
        String filePath = null;
        for (File f : files ) {
            filePath = f.getAbsolutePath();
            //System.out.println(f);
            if(f.isFile()){
                //parse(readFileToString(filePath));
            }
        }
    }

    public static boolean hasFinalVariablesNotInitialized(TypeDeclaration node){
        FieldDeclaration [] fields = node.getFields();


        for (FieldDeclaration field : fields) {
            if(hasFinalModifier(field)){

                for (Object fragment: field.fragments()){
                    if(fragment instanceof VariableDeclarationFragment){
                        if(((VariableDeclarationFragment) fragment).getInitializer() == null){
                            return true;
                        }
                    }

                }
            }
        }
        return false;
    }


//add empty constructor
    public static void addEmptyConstructorAndMakeClassPublic(TypeDeclaration node, boolean emptyConstructor) {
        if (!node.isInterface()) {

            int i = 0;
            while (i < node.modifiers().size()) {
                if (node.modifiers().get(i) instanceof Modifier) {
                    Modifier mod = (Modifier) node.modifiers().get(i);
                    if (mod.isPrivate() || mod.isProtected()) {
                        mod.setKeyword(ModifierKeyword.PUBLIC_KEYWORD);
                    }
                }
                i++;
            }
            if (node.modifiers().size() > 0 && !node.toString().contains("public ")) {

                Object firstMod = node.modifiers().get(0);
                if (firstMod instanceof Annotation) {
                    if (!((Modifier) node.modifiers().get(1)).isPublic()) {
                        node.modifiers()
                            .add(1, node.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
                    }

                } else if (firstMod instanceof Modifier) {
                    if (!((Modifier) firstMod).isPublic()) {
                        node.modifiers()
                            .add(0, node.getAST().newModifier(ModifierKeyword.PUBLIC_KEYWORD));
                    }
                }
            }

            if (hasEmptyConstructor(node) || hasFinalVariablesNotInitialized(node) || superclassesWithoutEmptyConstructor(node)) {
                System.out.println("It wasn't possible to add empty constructor to " + node.getName().getFullyQualifiedName());
                return;
            } else {
                AST ast = node.getAST();
                String className = node.getName().getFullyQualifiedName();
                MethodDeclaration newConstructor = ast.newMethodDeclaration();

                newConstructor.setName(ast.newSimpleName(className));
                newConstructor.setConstructor(true);
                newConstructor.setBody(ast.newBlock());
                ModifierKeyword amp = ModifierKeyword.PUBLIC_KEYWORD;
                newConstructor.modifiers().add(ast.newModifier(amp));

                node.bodyDeclarations().add(newConstructor);
            }
        }
    }

    private static boolean superclassesWithoutEmptyConstructor(TypeDeclaration node) {
        Type superClassType = node.getSuperclassType();
        if(superClassType == null)
            return false;
        TypeDeclaration superClassTypeDeclaration = superClassType.getAST().newTypeDeclaration();
        return !hasEmptyConstructor(superClassTypeDeclaration) || superclassesWithoutEmptyConstructor(superClassTypeDeclaration);
    }

    private static void changeClassAccessModifier(TypeDeclaration node){
        updatingAccessModifierForPublic(node.modifiers(), node.toString(), node.getAST());
    }

    //Remove remove Final modifiers and set private and protected modifiers
    private static void RemoveFinalModifierAndMakeFieldPublic(FieldDeclaration node) {
        updatingAccessModifierForPublic(node.modifiers(), node.toString(), node.getAST());
    }

    private static void updatingAccessModifierForPublic(List modifiers, String s, AST ast) {
        if (modifiers.size() > 0) {
            List<Modifier> modifiersToRemove = new ArrayList<Modifier>();
            int i = 0;

            while (i < modifiers.size()) {
                if (modifiers.get(i) instanceof Modifier) {
                    Modifier mod = (Modifier) modifiers.get(i);
                    /*if (mod.isFinal()) {
                        modifiersToRemove.add(mod);
                    } else */
                    if (mod.isPrivate() || mod.isProtected()) {
                        mod.setKeyword(ModifierKeyword.PUBLIC_KEYWORD);
                    }
                }
                i++;
            }
            for (Modifier mod : modifiersToRemove) {
                modifiers.remove(mod);
            }

            if (modifiers.size() > 0 && !s.contains("public ")) {

                Object firstMod = modifiers.get(0);
                if (firstMod instanceof Annotation) {
                    if (!((Modifier) modifiers.get(1)).isPublic()) {
                        modifiers
                            .add(1, ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
                    }

                } else if (firstMod instanceof Modifier) {
                    if (!((Modifier) firstMod).isPublic()) {
                        modifiers
                            .add(0, ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
                    }
                }
            }
        } else {
            modifiers
                .add(0, ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
        }
    }

    private static void RemoveFinalModifierAndMakeMethodPublic(MethodDeclaration node){
        updatingAccessModifierForPublic(node.modifiers(), node.toString(), node.getAST());
    }

    public static final void runTransformation(File file, boolean withTransformation, boolean fullTransformation) throws IOException {
        if (withTransformation) {
            final String str = FileUtils.readFileToString(file);
            org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(str);

            ASTParser parser = ASTParser.newParser(AST.JLS8);
            Map options = JavaCore.getOptions(); // New!
            JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options); // New!
            parser.setCompilerOptions(options);

            parser.setSource(document.get().toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);

            final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            transform(cu, fullTransformation);

            FileWriter fooWriter = new FileWriter(file, false); // true to append
            fooWriter.write(cu.toString());
            fooWriter.close();
        }
    }

    public static void main(String[] args) throws IOException {
        String path = args[0];
        File file = new File(path);
        Transformations.runTransformation(file, applyTransformations(args, 1), applyTransformations(args, 2));
    }

    private static boolean applyTransformations(String[] args, int index){
        if (args.length == 1){
            return true;
        }else if(Boolean.valueOf(args[index])){
            return true;
        }else{
            return false;
        }
    }

}
