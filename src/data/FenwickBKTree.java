package data;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.BitSet;

import static util.Utils.umiDist;

public class FenwickBKTree implements DataStructure{
    private Set<BitSet> s;
    private TreeMap<Integer, Integer> freqs;
    private int umiLength;
    private Node[] fenwick;

    @Override
    public void init(Map<BitSet, Integer> umiFreq, int umiLength, int maxEdits){
        this.s = umiFreq.keySet();
        this.umiLength = umiLength;

        freqs = new TreeMap<Integer, Integer>();

        for(Map.Entry<BitSet, Integer> e : umiFreq.entrySet())
            freqs.put(e.getValue(), null);

        int idx = 0;

        for(Integer key : freqs.keySet())
            freqs.put(key, idx++);

        fenwick = new Node[freqs.size() + 1]; // build Fenwick tree on frequencies

        for(Map.Entry<BitSet, Integer> e : umiFreq.entrySet()){
            BitSet umi = e.getKey();
            int freq = e.getValue();
            insert(umi, freq);
        }
    }

    @Override
    public List<BitSet> removeNear(BitSet umi, int k, int maxFreq){
        List<BitSet> res = new ArrayList<>();

        if(maxFreq != Integer.MAX_VALUE){ // always remove the queried UMI
            int freqIdx = freqs.size();

            for(; freqIdx > 0; freqIdx -= freqIdx & (-freqIdx))
                recursiveRemoveNear(umi, fenwick[freqIdx], 0, res);
        }

        Map.Entry<Integer, Integer> floorEntry = freqs.floorEntry(maxFreq);

        if(floorEntry == null)
            return res;

        int freqIdx = floorEntry.getValue() + 1;

        for(; freqIdx > 0; freqIdx -= freqIdx & (-freqIdx))
            recursiveRemoveNear(umi, fenwick[freqIdx], k, res);

        return res;
    }

    private void recursiveRemoveNear(BitSet umi, Node curr, int k, List<BitSet> res){
        int dist = umiDist(umi, curr.getUMI());

        if(dist <= k && curr.exists()){
            res.add(curr.getUMI());
            curr.setExists(false);
            s.remove(curr.getUMI());
        }

        int lo = Math.max(dist - k, 0);
        int hi = Math.min(dist + k, umiLength);
        boolean subtreeExists = curr.exists();

        for(int i = 0; i < umiLength + 1; i++){
            if(curr.subtreeExists(i)){
                if(i >= lo && i <= hi)
                    recursiveRemoveNear(umi, curr.get(i), k, res);

                subtreeExists |= curr.subtreeExists(i);
            }
        }

        curr.setSubtreeExists(subtreeExists);
    }

    private void insert(BitSet umi, int freq){
        int freqIdx = freqs.get(freq) + 1;

        for(; freqIdx <= freqs.size(); freqIdx += freqIdx & (-freqIdx)){
            if(fenwick[freqIdx] == null){
                fenwick[freqIdx] = new Node(umi);
            }else{
                Node curr = fenwick[freqIdx];
                int dist;

                do{
                    dist = umiDist(umi, curr.getUMI());
                }while((curr = curr.initNode(dist, umi, umiLength)) != null);
            }
        }
    }

    @Override
    public boolean contains(BitSet umi){
        return s.contains(umi);
    }

    @Override
    public Map<String, Float> stats(){
        Map<String, Float> res = new HashMap<>();

        double[] d = new double[3];

        for(Node curr : fenwick){
            if(curr != null){
                double[] a = depth(curr);
                d[0] += a[0];
                d[1] += a[1];
                d[2] += a[2];
            }
        }

        res.put("max depth", (float)d[1]);
        res.put("avg depth", (float)(d[2] / d[0]));
        return res;
    }

    private double[] depth(Node curr){
        double[] a = new double[3]; // num leaf nodes, max depth, depth sum

        boolean isLeaf = true;

        for(int i = 0; i < umiLength + 1; i++){
            if(curr.hasNode(i)){
                double[] b = depth(curr.get(i));
                a[0] += b[0];
                a[1] = Math.max(a[1], b[1] + 1);
                a[2] += b[2] + b[0];
                isLeaf = false;
            }
        }

        if(isLeaf){
            a[0] += 1;
            a[1] += 1;
            a[2] += 1;
        }

        return a;
    }

    private static class Node{
        private BitSet umi;
        private boolean exists, subtreeExists;
        private Node[] c;

        Node(BitSet umi){
            this.c = null;
            this.umi = umi;
            this.exists = true;
            this.subtreeExists = true;
        }

        Node initNode(int k, BitSet umi, int umiLength){
            if(c == null)
                c = new Node[umiLength + 1];

            if(c[k] == null){
                c[k] = new Node(umi);
                return null;
            }

            return c[k];
        }

        BitSet getUMI(){
            return umi;
        }

        boolean exists(){
            return exists;
        }

        void setExists(boolean exists){
            this.exists = exists;
        }

        void setSubtreeExists(boolean subtreeExists){
            this.subtreeExists = subtreeExists;
        }

        boolean subtreeExists(int k){
            return c != null && c[k] != null && c[k].subtreeExists;
        }

        Node get(int k){
            return c[k];
        }

        boolean hasNode(int k){
            return c != null && c[k] != null;
        }
    }
}
