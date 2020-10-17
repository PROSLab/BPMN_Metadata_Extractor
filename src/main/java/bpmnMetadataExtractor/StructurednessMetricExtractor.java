package bpmnMetadataExtractor;

import java.util.Vector;

import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

/**
 * In a block-structured process model, each split element has a corresponding join element of the
 * same type, and split-join pairs are properly nested
 */

public class StructurednessMetricExtractor {
	private double S;
	private Process process;
	private int graph;
	private int reducedGraph;
	private Vector<String> reducedNodes;
	private Vector<String> gateways;
	
	public StructurednessMetricExtractor(Process p) {
		this.process =p;
		this.graph = p.getChildElementsByType(FlowNode.class).size();
		this.reducedGraph = graph;
		this.reducedNodes = new Vector<String>();
		this.gateways = new Vector<String>();
	}
	
	public double getS() {
		return this.S;
	}
	
	public void setS() {
		for(Gateway split : process.getChildElementsByType(Gateway.class)) {
			this.reduceGraph(split);
		}
		//System.err.println(reducedGraph);
		this.S = 1.0 - (((double)reducedGraph) / ((double) graph));	
	}
	
	/*
	 * studia gli archi uscenti dallo split e ricerca un join 
	 */
	private int search(FlowNode fn, Vector<String> blocks) {
		int cont = 0;
		//check per verificare che il nodo non sia gi� ridotto
		if(!this.reducedNodes.contains(fn.getId())) {
			
			//check per verificare la presenza di uno split annidato
			if (fn instanceof Gateway && fn.getIncoming().size() == 1) {
				//chiamata ricorsiva al metodo che gestisce la ricerca di blocchi strutturati
				this.reduceGraph((Gateway) fn);
				}
			
			if(fn instanceof Gateway && !this.gateways.contains(fn.getId()) && !this.reducedNodes.contains(fn.getId())) {
				//se non � un join salva una stringa di avviso
				if(fn.getOutgoing().size() != 1)
					blocks.add("NoJoin");
				else blocks.add(fn.getId());
				return cont;
				}
			
			//se arriva all'end event il metodo termina
			if (fn instanceof EndEvent) {
				blocks.add(fn.getId());
				return cont;
			} 
			else
				//altrimenti cotinua la ricerca e aumenta il numero di nodi da ridurre 
				for(SequenceFlow sf : fn.getOutgoing()) {
					if(!this.reducedNodes.contains(fn.getId())) {
						cont++;
						this.reducedNodes.add(fn.getId());
						}
					else if(fn instanceof Gateway && this.gateways.contains(fn.getId())) {
						cont++;
						}	
					cont+=search(sf.getTarget(), blocks);
					}
			} 
		
		else for(SequenceFlow sf : fn.getOutgoing()) {
			cont += search(sf.getTarget(), blocks);	
		}
		
		return cont;
	}
		
	/*
	 * studia ogni gateway del grafo, elimina dal totale il numero di nodi che formano una struttura
	 */
	private void reduceGraph(Gateway split) {
		//check per verificare che il gateway esaminato sia uno split
		if(split.getIncoming().size() == 1 && !this.reducedNodes.contains(split.getId())) {
			//aggiunge il gateway alla lista di quelli esaminati
			this.reducedNodes.add(split.getId());
			int reduce = 0;
			//lista per salvare ogni join raggiunto dallo split in esame
			Vector<String> blocks = new Vector<String>();
			//salva il tipo di gateway
			String type = split.getElementType().getTypeName();
			
			for(SequenceFlow sf : split.getOutgoing())
				reduce+= this.search(sf.getTarget(), blocks);	
			//check per verificare che lo split in esame sia collegato unicamente ad un join
			boolean check = true;
			for(String id : blocks) {
				//System.out.println(id);
				if(!blocks.get(0).equals(id))
					check = false;
				}
			
			if(blocks.get(0) !="NoJoin" && check && type.equals(process.getModelInstance().getModelElementById(blocks.get(0)).getElementType().getTypeName())) {
				//salva l'id del join se compone una struttura
				this.gateways.addAll(blocks);
				//riduce il nodo corrispondente al join
				reduce++;
				this.reducedGraph = this.reducedGraph - reduce;
				//System.out.println(reduce);
				}
			
		} 
	}

}
