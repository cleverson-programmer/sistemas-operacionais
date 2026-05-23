package ProdutorConsumidor; // Define o pacote para manter Produtor junto das classes Buffer/Consumidor/ProdCons.
// Esta classe é uma Thread produtora: ela coloca valores no buffer e imprime o que colocou.
public class Produtor extends Thread { // Declara a thread do produtor, que executa concorrentemente com o consumidor.
    private Buffer buf; // Guarda a referência do buffer compartilhado; as chamadas put() geram prints do Buffer.
    private int numero; // Guarda o id do produtor para imprimir no console qual produtor produziu o valor.
    public Produtor(Buffer buf, int numero) { // Construtor que recebe o buffer compartilhado e o número identificador do produtor.
        this.buf = buf; // Armazena o buffer no campo para que o run() possa chamar buf.put() e gerar saídas no console.
        this.numero = numero; // Armazena o número do produtor para aparecer nos prints do tipo "Produtor #X inseriu: Y".
    } // Fecha o construtor; não imprime nada, apenas conclui a criação do objeto Produtor.
    public void run() { // Método executado quando start() é chamado; é aqui que a thread começa a produzir e imprimir.
        for (int i = 0; i < 5; i++) { // Repete 5 vezes para produzir 5 valores (0..4) que serão impressos e consumidos.
            buf.put(i); // Coloca o valor i no buffer (pode bloquear se estiver cheio) e imprime "Buffer recebeu (put): i".
            System.out.println("Produtor #" + numero + " inseriu: " + i); // Imprime que o produtor identificador "numero" inseriu o valor i.
            try { // Inicia try porque sleep pode lançar InterruptedException.
                sleep((int) (Math.random() * 100)); // Dorme por um tempo aleatório para variar a ordem dos prints entre produtor e consumidor.
            } catch (InterruptedException e) { // Captura interrupção para evitar erro e manter o exemplo simples.
            } // Fecha o catch; não imprime nada porque a interrupção é ignorada no exemplo base.
        } // Fecha o for; após produzir 5 valores, o produtor termina e não imprime mais nada.
    } // Fecha o run; ao terminar, a thread do produtor finaliza sua execução.
} // Fecha a classe Produtor; não imprime nada, apenas encerra a definição da thread produtora.
