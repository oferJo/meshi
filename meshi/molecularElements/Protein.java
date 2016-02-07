/*
 * Copyright (c) 2010. of Chen Keasar, BGU . For free use under LGPL
 */

package meshi.molecularElements;

import meshi.energy.simpleEnergyTerms.angle.AngleParametersList;
import meshi.energy.simpleEnergyTerms.bond.BondParametersList;
import meshi.geometry.AngleList;
import meshi.geometry.DistanceMatrix;
import meshi.geometry.TorsionList;
import meshi.molecularElements.atoms.Atom;
import meshi.molecularElements.atoms.AtomList;
import meshi.molecularElements.atoms.AtomPairList;
import meshi.molecularElements.atoms.MolecularSystem;
import meshi.molecularElements.ca.CaResidue;
import meshi.molecularElements.extendedAtoms.ResidueExtendedAtoms;
import meshi.parameters.SecondaryStructure;
import meshi.sequences.*;
import meshi.util.*;
import meshi.util.file.MeshiLineReader;
import meshi.util.file.MeshiWriter;
import meshi.util.filters.Filter;
import meshi.util.string.StringList;
import meshi.util.string.StringParser;

import java.io.File;
import java.util.Iterator;

/**
 * A protein chain.
 */
public class Protein implements Attributable {
    /**
     * The protein name.
     */
    private File sourceFile = null;
    public final MolecularSystem molecularSystem;

    public File sourceFile() {
        return sourceFile;
    }

    protected String name = "unkownProtein";

    /**
     * Often many models of the same protein are generated.
     */
    protected Integer modelNumber;
    /**
     * A list of the protein's residues.
     */
    protected ResidueList residues;
    protected ChainList chains;

    public ChainList chains() {
        return chains;
    }

    /**
     * A list of the protein's atoms.
     */
    protected AtomList atoms;
    /**
     * A list of the protein's bonds.
     */
    protected AtomPairList bonds = null;

    protected AngleList angles = null;
    protected TorsionList torsions = null;

    protected int firstResidueIndex = -1;

    private boolean verbose = false;
    //----------------------------------------  constructors -------------------------------------------

    public Protein(String name) {
        this.name = name;
        chains = new ChainList(this);
        molecularSystem = new MolecularSystem(DistanceMatrix.DistanceMatrixType.STANDARD);
    }

    /**
     * Builds a protein from AA sequence.
     */
    public Protein(MeshiSequence sequence,
                   String name, ResidueCreator creator) {
        this(sequence, name, Chain.GENERIC_CHAIN_NAME, creator);
    }

    public Protein(MeshiSequence sequence,
                   String name, String chainName, ResidueCreator creator) {
        this(name);
        chains = new ChainList(this);
        Chain chain = new Chain(sequence, creator, chainName, this);
        chains.add(chain); //add the chain as is without breaking it down
        residues = new ResidueList(chains);
        atoms = new AtomList(residues,molecularSystem);
        bonds = new AtomPairList(chains, null);
    }


    /**
     * Builds a protein from a PDB formatted file and a line filter.
     */
    public Protein(AtomList atomList, ResidueCreator creator) {
        this(atomList.comment());

        if (atomList.sourceFile() != null) sourceFile = atomList.sourceFile().file();
        else sourceFile = null;
        // MolecularSystem tempMolecularSystem = MolecularSystem.currentMolecularSystem();
        chains = new ChainList(atomList, creator, this);
        residues = new ResidueList(chains);
        atoms = new AtomList(residues,molecularSystem);


        bonds = new AtomPairList(chains, null);
        if (atoms.get(0).number() > atoms.size()) {
            throw new RuntimeException("The first atomOne of " + this + " is " + atoms.get(0) + ".\n" +
                    "It is kind of weird that its number is larger than the atomOne list size. This may not be a bug, but it was a bug in all the cases that it occurred to me (chen 3.4.2010).\n" +
                    "Are you sure that you handled Molecular system correctly?\n" +
                    "Specifically, did you create a new molecular system for this protein?\n" +
                    "The First atomOne of the molecular System is " + atoms.get(0).molecularSystem.get(0));
        }
    }

    /**
     * Builds a protein from a PDB formatted file and a line filter.
     */
    public Protein(String fileName, Filter filter, ResidueCreator creator, CommandList commands) {
        this(Utils.getProteinNameFromPdbFileName(fileName));

        //modelNumber = getModelNumber(fileName);  This needs to be fixed.

        AtomList tempAtoms = new AtomList(fileName, filter);
        //MolecularSystem.setCurrentMolecularSystem(tempMolecularSystem);
        sourceFile = new File(fileName);
        chains = new ChainList(tempAtoms, creator, this);
        residues = new ResidueList(chains);
        atoms = new AtomList(residues,molecularSystem);
        bonds = new AtomPairList(chains, commands);
    }

    public Protein(Residue residue) {
        this("temp");
        Chain chain = new Chain("A", this);
        chain.add(residue);
        chains.add(chain);
        residues = new ResidueList(chains);
        atoms = new AtomList(residues,molecularSystem);
        bonds = new AtomPairList(chains, null);
    }

    public void addChain(Chain chain) {
        chains.add(chain);
        residues = new ResidueList(chains);
        atoms = new AtomList(residues,molecularSystem);
        bonds = new AtomPairList(chains, null);
    }


    public Chain getChain(String chainName) {
        for (Chain chain: chains())
            if (chain.name().equals(chainName)) return chain;
        throw new RuntimeException("Chain "+chainName+" notFound");
    }

    public ResidueList missingResidues() {
        ResidueList out = new ResidueList();
        for (Iterator chainIter = chains.iterator(); chainIter.hasNext();) {
            Chain chain = (Chain) chainIter.next();
            for (Residue r : chain.missingResidues())
                out.add(r);
        }
        return out;
    }

    public AtomList nowhereAtoms() {
        AtomList out = new AtomList(molecularSystem);
        for (Iterator chainIter = chains.iterator(); chainIter.hasNext();) {
            Chain chain = (Chain) chainIter.next();
            for (Atom a : chain.nowhereAtoms(molecularSystem))
                out.add(a);
        }
        return out;
    }

    //------------


    /**
     * Builds a protein from a list of atoms and a ResidueCreator.
     * Allows Forcefield specific atoms and residued. 
     public Protein(AtomList atomList, ResidueCreator creator) throws MissingResiduesException {
     new MolecularSystem();
     name = getProteinName(atomList.sourceFile());
     modelNumber = getModelNumber(atomList.sourceFile());
     chains = new ChainList(atomList.filter(new OneOfTwenty()),
     creator);
     residues = new ResidueList(chains);
     atoms = new AtomList(residues);
     bonds = new AtomPairList(chains);
     }
     **/

    /**
     * Builds a protein from a list of atoms.
     * This constructor is used by HeliumClusterMinimization our work horse for
     * development. It is not very useful in any other context.
     */
    public Protein(AtomList atomList) {
        molecularSystem = new MolecularSystem();
        name = getProteinName(atomList.sourceFile());
        modelNumber = getModelNumber(atomList.sourceFile());
        residues = null;
        atoms = atomList;
        bonds = null;
    }


    //-------------------------------------------- Methods ----------------------------------------
//     public void updateAtomList() {
// 	atoms =  new AtomList(residues);
//     }

//     public void updateBondsList() {
// 	        bonds = new AtomPairList(chains);
//     }

    public AtomPairList bonds() {
        return bonds;
    }


    /**
     * Extract the protein name from the filename.
     * expects name.pdb or name.modelNumber.pdb or pdbname.ent
     */
    public static String getProteinName(MeshiLineReader file) {
        if (file == null) return "unKnown";
        return (new File(getProteinName(file.path()))).getName();
    }

    public static String getProteinName(String pathString) {
        StringList path = StringParser.breakPath(pathString);
        String fileName = path.get(path.size() - 1);
        StringList temp = StringParser.breakFileName(fileName);
        String name;
        if (temp.size() <= 2) name = temp.get(0);
            // was 	else  name = temp.get(0)+"."+temp.get(1);
        else {
            name = "";
            for (int i = 0; i < temp.size() - 1; i++)
                name += temp.get(i) + (i < temp.size() - 2 ? "." : "");
        }
        if (name.startsWith("pdb"))
            name = name.substring(3);
        return (name);
    }

    //------------


    /**
     * Extracts the model number from the filename.
     * Expects file name with the format name.modelNumber.pdb
     */
    private Integer getModelNumber(MeshiLineReader file) {
        if (file == null) return new Integer(0);
        StringList path = (StringParser.breakPath(file.path()));
        String fileName = path.get(path.size() - 1);
        if ((StringParser.breakFileName(fileName)).size() >= 3) {
            try {
                return new Integer((StringParser.breakFileName(fileName)).get(1));
            }
            catch (Exception e) {
                return new Integer(0);
            }
        } else return new Integer(0);
    }

    /**
     * The protein name.
     */
    public String name() {
        return name;
    }

    public int modelNumber() {
        if (modelNumber == null) return 0;
        return modelNumber.intValue();
    }

    /**
     * A list of the protein's atoms.
     */
    public AtomList atoms() {
        return atoms;
    }

    /**
     * A list of the protein's residues.
     */
    public ResidueList residues() {
        return residues;
    }


    /**
     * An array of the Residue Identifiers of the first residues in each chain
     * In most cases it is residue 1 but in many other cases some n > 1 typicaly because
     * the N-terminus is not present in the PDB file
     */
    public ResidueIdentifier[] firstResidues() {
        ResidueIdentifier[] out = new ResidueIdentifier[chains.size()];
        int i = 0;
        for (Iterator chainIter = chains.iterator(); chainIter.hasNext();) {
            Chain chain = (Chain) chainIter.next();
            boolean found = false;
            for (Iterator residues = chain.iterator(); residues.hasNext() & (!found);) {
                Residue residue = (Residue) residues.next();
                if (!residue.dummy()) {
                    out[i] = residue.ID();
                    found = true;
                }
            }
            if (!found) throw new RuntimeException("Cahin " + chain + " has no residues");
        }
        return out;
    }

    /**
     * Returns the residue.
     */
    public Residue residue(ResidueIdentifier id) {
        return residues.residue(id);
    }

    /**
     * Returns the residue.
     */
    public Residue residue(int residueNumber) {
        if (chains.size() > 1)
            throw new RuntimeException("Cannot pick a residue by its number. There are " + chains.size() + " chains");
        String chain = chains.get(0).get(0).ID().chain();
        return residues.residue(new ResidueIdentifier(chain, residueNumber));
    }


    /**
     * Allow all atoms to move.
     */
    public void defrost() {
        atoms.defrost();
    }

    public void freeze() {
        atoms.freeze("In Protein");
    }

    public void freeze(Filter filter) {
        atoms.freeze(filter);
    }

    public String toString() {
        return name;
    }

    /**
     * Returns the specified atomOne.
     */
    public Atom getAtom(String residueName, ResidueIdentifier residueID, String atomName) {
        Iterator residuesIter = residues.iterator();
        Residue residue;
        Iterator atoms;
        Atom atom;
        while ((residue = (Residue) residuesIter.next()) != null) {
            if (residue.ID().equals(residueID)) {
                if (!(residue.type().nameThreeLetters().equals(residueName)))
                    throw new RuntimeException("Weird " + residue + " " + residue.ID() + " " + residueName);
                atoms = residue.atoms.iterator();
                while ((atom = (Atom) atoms.next()) != null)
                    if (atom.name.equals(atomName)) {
                        return atom;
                    }
            }
        }
        return null;
    }

    /**
     * For users who need this signature.
     */
// 	public Atom getAtom(String residueName, int residueNumber, String atomName){
// 		return getAtom(residueName, new DummyResidue(residueNumber).ID(), atomName);
// 	}

// 	public void setResidues(ResidueList residueList) {
// 	residues = residueList;
// 	atoms = new AtomList(residues);
// 	bonds = new AtomPairList(residues); 
//     }
    public int firstResidueIndex() {
        return firstResidueIndex;
    }

    public void allYouWantToKnow() {
        System.out.println("##################################################################################\n" +
                "     Anything you ever wanted to know about " + this + " and never dared to ask\n" +
                "##################################################################################\n");
        System.out.println("========= residues ===========");
        Utils.print(residues());
        System.out.println("========= atoms  ===========");
        Utils.print(atoms());
        System.out.println("========= bonds ===========");
        Utils.print(bonds());
    }


    /**
     * A method to setResidue the secondary structure of a protein.
     * The input SS string currently support only C,E,H letters + A for the ALL type (= every SS is
     * posible). This method works only if the number of letter in the SS string equals to the
     * number of non-dummy residues. The assignment of SS is than sequential in the residue numbers.
     */
    public void setSS(String SS) {

        int cc = -1; // residue number 0 is always dummy
        int resNum = -99999999;

        for (Residue res : residues) {
            if (!res.dummy()) {
                // Checking that the residues in the list are ordered by number
                if (resNum == -99999999)
                    resNum = res.ID().number();
                else {
                    if (resNum >= res.ID().number())
                        throw new RuntimeException("\n\nThe residues in the residue list are not" +
                                " sorted by ascending residue number\n\n");
                    else
                        resNum = res.ID().number();
                }
                // Assigning the SS
                if (cc == SS.length())
                    throw new RuntimeException("\n\nThe secondary structure string provided is not long enough. \n\n");
                res.setSecondaryStructure(SecondaryStructure.secondaryStructure(SS.charAt(cc)));
            }
            cc++;
        }
        //preint the new residue list.
        Utils.print(residues, 5, " %-20s ");
    }




    public String getSequence() {
        String ans = "";
        boolean first = true;
        for (Residue res : residues) {
            if (first) first = false;// to ignore the first 0th residue, which is always dummy.
            else {
                if (res.dummy()) ans += "-";
                else ans += res.type().nameOneLetter();
            }
        }
        return ans;
    }


    public void printAtomsToFile(String fileName) throws Exception {

        MeshiWriter mw = new MeshiWriter(fileName);
        atoms.print(mw);
        mw.close();
    }

    public void setName(String name) {
        this.name = name;
    }


    public Residue residueAt(int residueNumber) {
        return residues.get(residueNumber);
    }

    public MeshiSequence sequence() {
        if (chains.size() > 1)
            throw new RuntimeException("A protein with " + chains.size() + " chains. More than one sequence");
        return chain().sequence();
    }

    public Chain chain() {
        if (chains.size() > 1)
            throw new RuntimeException("The method Protein.chain() cannot be applied to " + this.name() + ". This method is intended to be used only when the protein has only one chain.");
        return chains.get(0);
    }


    /**
     * Sets the chain of <code>this</code> protein, after asserting
     * that it is comprised of residues belonging to a single
     * polypeptide chain.
     */
// 	public void setChain(String chain) {
// 		residues.setChain(chain);
// 	}


    //================================= filters =========================================
    public static class BackboneFilter implements Filter {
        public boolean accept(Object obj) {
            if (((Atom) obj).name.equals("CA")) return true;
            if (((Atom) obj).name.equals("CB")) return true;
            if (((Atom) obj).name.equals("C")) return true;
            if (((Atom) obj).name.equals("N")) return true;
            if (((Atom) obj).name.equals("O")) return true;
            if (((Atom) obj).name.equals("H")) return true;
            return false;
        }
    }
    //------------------------------------------------------------------------------------------------------------------------------------------------

    public static Protein getCAproteinFromApdbFile(File file) {
        //MolecularSystem saveMS = MolecularSystem.currentMolecularSystem();
        new MolecularSystem();
        AtomList tempAtomList = new AtomList(file.getAbsolutePath());
        if (tempAtomList.size() == 0)
            throw new RuntimeException("No atoms if file " + file + " " + file.getAbsolutePath());
       // MolecularSystem.setCurrentMolecularSystem(saveMS);
        //saveMS = MolecularSystem.currentMolecularSystem();
        new MolecularSystem();
        Protein out = new Protein(tempAtomList, CaResidue.creator);
        //MolecularSystem.setCurrentMolecularSystem(saveMS);
        return out;
    }

    public static Protein getExtendedAtomsProteinFromPdbFile(File file) {
        return getExtendedAtomsProteinFromPdbFile(file, null, null);
    }

    public static Protein getExtendedAtomsProteinFromPdbFile(File file,
                                                             BondParametersList bondParametersList,
                                                             AngleParametersList angleParametersList) {
        //MolecularSystem saveMS = MolecularSystem.currentMolecularSystem();
        new MolecularSystem();
        AtomList tempAtomList = new AtomList(file.getAbsolutePath());

        new MolecularSystem();
        Protein out = new Protein(tempAtomList, ResidueExtendedAtoms.creator);
        //MolecularSystem.setCurrentMolecularSystem(saveMS);
        if ((bondParametersList != null) & (angleParametersList != null))
            ResidueExtendedAtoms.addHydrogens(out, bondParametersList, angleParametersList);
        return out;
    }

//---------------------- attributable ----------------------------------------
    private AttributesRack attributes = new AttributesRack();

    public final void addAttribute(MeshiAttribute attribute) {
        attributes.addAttribute(attribute);
    }

    public final MeshiAttribute getAttribute(int key) {
        return attributes.getAttribute(key);
    }

}