import java.util.*;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}
	private String[] Codes;
	private int b;
	private int[] Count;
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	//follow pseudo-code given
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = readForCounts(in, out);
		HuffNode root = makeFromCounts(counts);
		String[] codings = makeFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	
	
	private int[] readForCounts(BitInputStream in, BitOutputStream out) {
		int[] counts = new int[(ALPH_SIZE + 1)];
		counts[PSEUDO_EOF] = 1;
		
		while(true) {			
			int bits = in.readBits(BITS_PER_WORD);			
			if (bits == -1) break;
			counts[bits]++;
		}
		return counts;
	}
	
	
	private HuffNode makeFromCounts(int[] counts) {
		
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();

		for(int i = 0; i < counts.length; i ++ ) {
			if (counts[i] > 0) {
				pq.add(new HuffNode(i, counts[i], null, null)); 
			}  
		}

		while (pq.size() > 1) {
		    HuffNode left = pq.remove();
		    HuffNode right = pq.remove();
		    pq.add(new HuffNode(0, left.myWeight + right.myWeight, left, right)); 
		}
		
		HuffNode head = pq.remove();
		return head;
	}
	
	
	private String[] makeFromTree(HuffNode root) {
		String[] Codes = new String[ALPH_SIZE + 1];
	    makecodes(root,"",Codes);
	    return Codes;
	}
	
	private void makecodes(HuffNode root, String path, String[] Codes) {
		if (root == null) return;
		if (root.myLeft ==null && root.myRight==null) {
			Codes[root.myValue]= path; 
			return; 
		}
		makecodes(root.myLeft,path + "0", Codes); 
		makecodes(root.myRight,path + "1", Codes);
	}
	
	private void writeHeader(HuffNode root, BitOutputStream out) {
		 
		if(root.myLeft == null && root.myRight == null) {
			
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.myValue);			
			return;
		}
		out.writeBits(1, 0);
		writeHeader(root.myLeft, out);
		writeHeader(root.myRight, out);
	}
	
	private void writeCompressedBits(String[] codes, BitInputStream in, BitOutputStream out) {
		while (true) {
			int bits = in.readBits(BITS_PER_WORD);
			if (bits == -1) break;
			String code = codes[bits];
				out.writeBits(code.length(), Integer.parseInt(code,2));
		}
		String code = codes[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code,2));
	}
//	public void compress(BitInputStream in, BitOutputStream out) {
//		Count = new int[ALPH_SIZE]; //readForCounts helpercode
//		Count[PSEUDO_EOF] = 1;
//		while (true) {
//			int bits = in.readBits(BITS_PER_WORD);
//			if (bits == -1) break;
//			Count[b] = Count[b] + 1;
//		}
//		//in.reset();
//
//		PriorityQueue<HuffNode> pq = new PriorityQueue<HuffNode>(); //makeTreefromCounts code
//		for (int k = 0; k < ALPH_SIZE; k++) {
//			if (Count[k] != 0) {
//				pq.add(new HuffNode(k, Count[k], null, null));
//			}
//		}
//		//pq.add(new HuffNode(PSEUDO_EOF, 0));
//
//		while (pq.size() > 1) {
//			HuffNode left = pq.remove();
//			HuffNode right = pq.remove();
//			pq.add(new HuffNode(0, right.myWeight + left.myWeight, left, right));
//		}
//		HuffNode myHead = pq.remove();
//		
//		
//		//next step
//		String[] codings = makeFromTree(myHead);
//		
//		out.writeBits(BITS_PER_INT, HUFF_TREE);
//		writeHeader(myHead, out);
//		
//		in.reset();
//		writeCompressedBits(codings, in, out);
//		out.close();
//	}
//	
//	private String[] makeFromTree(HuffNode root) {
//		String[] encodings = new String[ALPH_SIZE + 1];
//	    codingHelper(root,"",encodings);
//	    return encodings;
//	    
//
//	}
//	
//	private void codingHelper(HuffNode root, String path, String[] encodings) {
//		if (root == null) return;
//		if (root.myLeft ==null && root.myRight==null) {
//			encodings[root.myValue]= path; 
//			return; 
//		}
//		
//		codingHelper(root.myLeft,path + "0", encodings); 
//		codingHelper(root.myRight,path + "1", encodings);
//
//	}
//	
//	private void writeHeader(HuffNode root, BitOutputStream out) {
//		 
//		if(root.myLeft == null && root.myRight == null) { // if leafnode then 
//			
//			out.writeBits(1, 1); // single bit of one
//			out.writeBits(BITS_PER_WORD + 1, root.myValue); // write nine bits
//			
//			return;
//			
//		}
//		
//		out.writeBits(1, 0); // single bit of zero
//		writeHeader(root.myLeft, out);
//		writeHeader(root.myRight, out);
//		
//	}
//	
//	private void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
//		while (true) {
//			int bits = in.readBits(BITS_PER_WORD);
//			if (bits == -1) break;
//			String code = codings[bits];
//				out.writeBits(code.length(), Integer.parseInt(code,2));
//
//		}
//		String code = codings[PSEUDO_EOF];
//		out.writeBits(code.length(), Integer.parseInt(code,2));
//
//	}
//	
//	private void extractCodes(HuffNode current, String path) {
//		if ((current.myLeft == null) && (current.myRight == null)) {
//			Codes[current.myValue] = path;
//			return;
//		}
//		extractCodes(current.myLeft, path + 0);
//		extractCodes(current.myLeft, path + 1);
//	}
//
//	private void writeHeader(HuffNode current, BitOutputStream out) {
//		if ((current.myLeft == null) && (current.myRight == null)) {
//			out.writeBits(1, 1);
//			out.writeBits(9, current.myValue);
//		} else {
//			out.writeBits(1, 0);
//			writeHeader(current.myLeft, out);
//			writeHeader(current.myRight, out);
//		}
//	}

	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	
	public void decompress(BitInputStream in, BitOutputStream out) {
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with " + bits);			
		}
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}
	
	private HuffNode readTreeHeader(BitInputStream in) {
		int bits = in.readBits(1);
		if (bits == -1) {
			throw new HuffException("failed");
		}
		
		if (bits == 0) {
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader(in); // is this it lol
			return new HuffNode(0, 0, left, right);
		}
		else {
			int value = in.readBits(BITS_PER_WORD+1);
			return new HuffNode(value, 0, null, null);
		}
	}
	
	private void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		
		HuffNode current = root;
		while (true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			else {
				if (bits == 0) current = current.myLeft;
				else current = current.myRight;
				
				if (current.myLeft == null && current.myRight == null) {
					if (current.myValue == PSEUDO_EOF) break;
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root; // start back after leaf
					}
				}
			}
		}
//		in.close();
//		out.close();
	}
}