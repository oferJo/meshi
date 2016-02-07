/*
 * Copyright (c) 2010. of Chen Keasar, BGU . For free use under LGPL
 */

package meshi.util.info;

import meshi.molecularElements.Protein;
import meshi.molecularElements.atoms.MolecularSystem;

/**

 */
public class ProteinInfo extends MeshiInfoElementList {
    private String name, fileName, comment;
    private Protein protein;
    public final String BLANK30 = "                              ";
    private double sum;

    public double sum() {
        return sum;
    }

    public void setSum(double s) {
        sum = s;
    }

    private boolean on;

    public boolean on() {
        return on;
    }

    public void turnOn() {
        on = true;
    }

    public void turnOff() {
        on = false;
    }

    private ProteinInfoHeader header;

    public ProteinInfo(String name, String fileName, String comment, Protein protein) {
        this.name = name;
        this.fileName = fileName;
        new MolecularSystem();
        this.comment = comment;
        on = true;
        this.protein = protein;
        header = new ProteinInfoHeader(name, fileName, comment, protein);
    }

    public String originalModel() {
        int index = fileName.indexOf("TS");
        return fileName.substring(0, index + 2);
    }

    public ProteinInfo(ProteinInfo proteinInfo) {
        this(proteinInfo, proteinInfo.name + ".copy", proteinInfo.fileName, "A deep copy of " + proteinInfo.comment + ". ");
        add(deepCopy(proteinInfo));
    }


    public String name() {
        return name;
    }

    public String fileName() {
        return fileName;
    }

    public String header() {
        String out = F("name");
        for (MeshiInfoElement element : this)
            out += F(element.type.toString());
        out += "comment";
        return out;
    }

    public String values() {
        String out = F(name);
        for (MeshiInfoElement element : this) {
            switch (element.valueType()) {
                case INTEGER:
                    out += F(new Integer(((IntInfoElement) element).value()).toString());
                    break;
                case DOUBLE:
                    out += F(new Double(((DoubleInfoElement) element).value()).toString());
                    break;
                case STRING:
                    out += F(((StringInfoElement) element).value());
                    break;
                default:
                    throw new RuntimeException("Do not know how to handle " + element + "of value type" + element.valueType());
            }
        }
        out += comment;
        out = out.replaceAll("\"","");
        return out;
    }

    public String toXML(int indentationTabs) {
        return super.toXML(indentationTabs, header, "</" + header.type + ">");
    }

    public ProteinInfo(ProteinInfo proteinInfo, String name, String fileName, String comment) {
        this(name, fileName, comment, proteinInfo.protein);
        for (MeshiInfoElement element : proteinInfo) {
            switch (element.valueType()) {
                case INTEGER:
                    add(new IntInfoElement((IntInfoElement) element));
                    break;
                case DOUBLE:
                    add(new DoubleInfoElement((DoubleInfoElement) element));
                    break;
                case STRING:
                    add(new StringInfoElement((StringInfoElement) element));
                    break;
                default:
                    throw new RuntimeException("Do not know how to handle " + element + "of value type" + element.valueType());
            }
        }
    }

    public void toZero() {
        for (MeshiInfoElement element : this) {
            switch (element.valueType()) {
                case INTEGER:
                    ((IntInfoElement) element).toZero();
                    break;
                case DOUBLE:
                    ((DoubleInfoElement) element).toZero();
                    break;
            }
        }
    }

    public void addElementValue(ProteinInfo proteinInfo) {
        for (int i = 0; i < size(); i++) {
            MeshiInfoElement element = proteinInfo.get(i);
            MeshiInfoElement myElement = get(i);
            if (element.type != myElement.type)
                throw new RuntimeException("Cannot perform a binary operation reset two elements of different types" +
                        myElement + " of type " + myElement.type + "and " + element + " of type " + element.type);
            if (element.valueType() == InfoValueType.DOUBLE)
                ((DoubleInfoElement) get(i)).addValue((DoubleInfoElement) element);
        }
    }

    public void addElementValue2(ProteinInfo proteinInfo) {
        for (int i = 0; i < size(); i++) {
            MeshiInfoElement element = proteinInfo.get(i);
            if (element.valueType() == InfoValueType.DOUBLE)
                ((DoubleInfoElement) get(i)).addValue2((DoubleInfoElement) element);
        }
    }

    public void divideBy(ProteinInfo proteinInfo) {
        for (int i = 0; i < size(); i++) {
            MeshiInfoElement element = proteinInfo.get(i);
            if (element.valueType() == InfoValueType.DOUBLE)
                ((DoubleInfoElement) get(i)).divideBy((DoubleInfoElement) element);
        }
    }

    public void divideBy(double div) {
        for (MeshiInfoElement element : this)
            if (element.valueType() == InfoValueType.DOUBLE) ((DoubleInfoElement) element).divideBy(div);
    }

    public void square() {
        for (MeshiInfoElement element : this)
            if (element.valueType() == InfoValueType.DOUBLE) ((DoubleInfoElement) element).square();
    }

    public void sqrt() {
        for (MeshiInfoElement element : this)
            if (element.valueType() == InfoValueType.DOUBLE) ((DoubleInfoElement) element).sqrt();
    }

    public void subtract(ProteinInfo proteinInfo) {
        for (int i = 0; i < size(); i++) {
            MeshiInfoElement element = proteinInfo.get(i);
            if (element.valueType() == InfoValueType.DOUBLE)
                ((DoubleInfoElement) get(i)).subtract((DoubleInfoElement) element);
        }
    }

    public String toString() {
        return header() + "\n" + values();
    }


    private String F(String s) {
        return (s + BLANK30).substring(0, 30) + "\t";
    }
}

