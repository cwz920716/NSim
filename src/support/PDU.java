// PDU.java

package support;				// protocol support package

public class PDU {
	
	public static final int NIL = 0;
	public static final int IP = 1;
	public static final int TCP = 2;
	public static final int ECMP = 3;
	public static final int DARD = 4;
	public static final int DARDCTRL = 5;
	public static final int LBDAR = 6;
	public static final int FCTRL = 7;
	public static final int DETOUR = 8;

	/** Protocol Data Unit type */
	public int type;

	/** Protocol Data Unit user data length */
	public int size = 0;
	
	/** Protocol Data payload type */
	public int subtype;

	/** Protocol Data Unit payload as Service Data Unit */
	public PDU sdu = null;

	/**
    Constructor for a PDU object.

    @param type		PDU type
    @param sdu		PDU payload as SDU
	 */
	public PDU(int type, int size, int subtype, PDU sdu) {
		this.type = type;
		this.size = size;
		this.subtype = subtype;
		this.sdu = sdu;
	}

	/**
    Convert a PDU to a string representation.

    @return		string representation of a PDU
	 */
	public String toString() {
		return ("PDU <Type " + type + ", Size " + size + ", Subtype " + subtype + ", Data " + sdu.toString() + ">");
	}
	
	/**
    Convert a PDU to a hash code representation.

    @return		hash code of a PDU
	 */
	public int hash(int k) {
		return (((Integer) size).hashCode() + ((sdu == null) ? 0 : sdu.hash(k))) % k;
	}

}

