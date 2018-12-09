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
	private String[] charCodes;
	private int bits;
	private int[] charCount;
	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out) {
		charCount = new int[ALPH_SIZE];
		bits = in.readBits(BITS_PER_WORD);
		while (bits != -1) {
			charCount[bits] = charCount[bits] + 1;
			bits = in.readBits(BITS_PER_WORD);
		}
		in.reset();

		PriorityQueue<HuffNode> pq = new PriorityQueue<HuffNode>();
		for (int k = 0; k < ALPH_SIZE; k++) {
			if (charCount[k] != 0) {
				pq.add(new HuffNode(k, charCount[k]));
			}
		}
		pq.add(new HuffNode(PSEUDO_EOF, 0));

		while (pq.size() > 1) {
			HuffNode left = pq.poll();
			HuffNode right = pq.poll();
			pq.add(new HuffNode(-1, right.myWeight + left.myWeight, left, right));
		}
		HuffNode myHead = pq.poll();

		charCodes = new String[ALPH_SIZE + 1];
		extractCodes(myHead, "");

		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeHeader(myHead, out);

		bits = in.readBits(BITS_PER_WORD);
		while (bits != -1) {
			String myCode = charCodes[bits];
			out.writeBits(myCode.length(), Integer.parseInt(myCode, 2));
			bits = in.readBits(BITS_PER_WORD);
		}

		// Write pseudo-EOF
		String endFile = charCodes[PSEUDO_EOF];
		out.writeBits(endFile.length(), Integer.parseInt(endFile, 2));	
	}
	
	// Get compressed codes for each character that appears
	private void extractCodes(HuffNode current, String path) {
		if ((current.myLeft == null) && (current.myRight == null)) {
			charCodes[current.myValue] = path;
			return;
		}
		extractCodes(current.myLeft, path + 0);
		extractCodes(current.myLeft, path + 1);
	}

	// Create a compressed file's header containing tree of HuffNodes
	private void writeHeader(HuffNode current, BitOutputStream out) {
		if ((current.myLeft == null) && (current.myRight == null)) {
			out.writeBits(1, 1);
			out.writeBits(9, current.myValue);
		} else {
			out.writeBits(1, 0);
			writeHeader(current.myLeft, out);
			writeHeader(current.myRight, out);
		}
	}
//	public void compress(BitInputStream in, BitOutputStream out) {
//		int[] counts = readForCounts(in);
//		HuffNode root = makeTreeFromCounts(counts);
//		String[] codings = makeCodingsFromTree(root);
//		
//		out.writeBits(BITS_PER_INT, HUFF_TREE);
//		writeHeader(root, out);
//		
//		in.reset();
//		writeCompressedBits(codings, in, out);
//		out.close();
//	}
//	
//	public int[] readForCounts(BitInputStream in) {
//		
//	}
//	
//	public HuffNode makeTreeFromCounts(int[] counts) {
//		
//		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
//		
//		for(asdf) {
//			pq.add(new HuffNode(index, freq[index]), null, null);
//		}
//		
//		while (pq.size() > 1) {
//			HuffNode left = pq.remove();
//			HuffNode right = pq.remove();
//			// create new HuffNode t with weight from
//			// left.weight+right.weight and left, right subtrees
//			pq.add(t);
//		}
//		
//		
//		HuffNode root = pq.remove();
//	}
//	
//	public String[] makeCodingsFromTree(HuffNode root) {
//		
//	}
//	
//	public void writeHeader(HuffNode root, BitOutputStream out) {
//		
//	}
//	
//	public void writeCompressedBits(String[] codings, BitInputStream in, BitOutputStream out) {
//		
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
		if (bits != HUFF_TREE || bits == -1) {
			throw new HuffException("illegal header starts with " + bits);			
		}
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}
	
	public HuffNode readTreeHeader(BitInputStream in) {
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE || bits == -1) {
			throw new HuffException("illegal header starts with " + bits);
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
	
	public void readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		
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
						out.writeBits(BITS_PER_WORD+1, current.myValue);
						current = root; // start back after leaf
					}
				}
			}
		}
		
	}
}