package ProdutorConsumidor; // Define o pacote para manter Consumidor junto das classes Buffer/Produtor/ProdCons.
// Esta classe é uma Thread consumidora: ela retira valores do buffer e imprime o que retirou.
class Consumidor extends Thread { // Declara a thread do consumidor, que executa concorrentemente com o produtor.
    private Buffer buf; // Guarda a referência do buffer compartilhado; as chamadas get() geram prints do Buffer.
    private int numero; // Guarda o id do consumidor para imprimir no console qual consumidor consumiu o valor.
    public Consumidor(Buffer buf, int numero) { // Construtor que recebe o buffer compartilhado e o número identificador do consumidor.
        this.buf = buf; // Armazena o buffer no campo para que o run() possa chamar buf.get() e gerar saídas no console.
        this.numero = numero; // Armazena o número do consumidor para aparecer nos prints do tipo "Consumidor #X removeu: Y".
    } // Fecha o construtor; não imprime nada, apenas conclui a criação do objeto Consumidor.
    public void run() { // Método executado quando start() é chamado; é aqui que a thread começa a consumir e imprimir.
        int x; // Declara a variável que receberá o valor lido do buffer para depois ser impresso pelo consumidor.
        for (int i = 0; i < 5; i++) { // Repete 5 vezes para consumir 5 valores (normalmente 0..4) que foram produzidos.
            x = buf.get(); // Retira um valor do buffer (pode bloquear se estiver vazio) e imprime "Buffer entregou (get): x".
            System.out.println("Consumidor #" + numero + " removeu: " + x); // Imprime que o consumidor identificador "numero" removeu o valor x.
        } // Fecha o for; após consumir 5 valores, o consumidor termina e não imprime mais nada.
    } // Fecha o run; ao terminar, a thread do consumidor finaliza sua execução.
} // Fecha a classe Consumidor; não imprime nada, apenas encerra a definição da thread consumidora.
