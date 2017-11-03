/**
 * Created by Josue Cubero on 07/12/2016.
 */
public class Triad {
    private String word;
    private float frecuencies;
    private float probability;

    public Triad(String w, float f, float p){
        word = w;
        frecuencies = f;
        probability = p;
    }

    public String[] get_triad_data(){
        String[] data = {word, Float.toString(frecuencies), Float.toString(probability)};
        return data;
    }

    public String getWord(){
        return word;
    }

    public float getFrecuency(){
        return frecuencies;
    }

    public float getProbability(){
        return probability;
    }


    public void setFrecuencies(float f){
        frecuencies += f;
    }

    public void setProbability(float p){
        probability = p;
    }
}
