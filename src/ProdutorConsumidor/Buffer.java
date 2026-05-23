package ProdutorConsumidor; // Define o pacote para agrupar as classes do exemplo Produtor-Consumidor.
// Esta classe representa o buffer compartilhado (neste exemplo, buffer de 1 posição).
public class Buffer { // Declara a classe Buffer, que contém o recurso compartilhado entre produtor e consumidor.
    private int valor; // Guarda o valor atualmente armazenado no buffer (o número que será impresso nos logs de put/get).
    private boolean cheio = false; // Indica se o buffer está cheio; quando false, consumidor espera; quando true, produtor espera.
    public synchronized int get() { // Método sincronizado para o consumidor retirar um valor; imprime quando entrega o valor.
        while (!cheio) { // Enquanto o buffer estiver vazio, o consumidor não pode retirar e ficará esperando sem busy wait.
            try { // Inicia um bloco try porque wait() pode lançar InterruptedException.
                wait(); // Libera o lock do Buffer e bloqueia a thread do consumidor até alguém chamar notifyAll() após um put.
            } catch (InterruptedException e) { // Captura interrupção para evitar erro e manter a thread viva neste exemplo.
            } // Fecha o catch; aqui não imprime nada porque a interrupção é ignorada no exemplo base.
        } // Fecha o while; ao sair, significa que cheio==true e existe um valor pronto para ser consumido.
        cheio = false; // Marca o buffer como vazio porque o valor será consumido agora (isso permite que o produtor coloque outro).
        notifyAll(); // Acorda threads esperando (principalmente o produtor) para que ele possa continuar e fazer o próximo put.
        System.out.println("Buffer entregou (get): " + valor); // Imprime o valor que está sendo entregue ao consumidor (linha aparece no console).
        return valor; // Retorna o valor ao consumidor, que imprimirá outro log confirmando a remoção.
    } // Fecha o método get; não imprime ao fechar, apenas delimita o fim do método.
    public synchronized void put(int valor) { // Método sincronizado para o produtor inserir um valor; imprime quando recebe o valor.
        while (cheio) { // Enquanto o buffer estiver cheio, o produtor não pode inserir e ficará esperando sem busy wait.
            try { // Inicia um bloco try porque wait() pode lançar InterruptedException.
                wait(); // Libera o lock do Buffer e bloqueia a thread do produtor até alguém chamar notifyAll() após um get.
            } catch (InterruptedException e) { // Captura interrupção para evitar erro e manter a thread viva neste exemplo base.
            } // Fecha o catch; aqui não imprime nada porque a interrupção é ignorada no exemplo base.
        } // Fecha o while; ao sair, significa que cheio==false e há espaço (a única posição) para colocar um novo valor.
        this.valor = valor; // Copia o valor produzido para dentro do buffer, para que ele seja impresso e depois consumido.
        cheio = true; // Marca o buffer como cheio para indicar que há um valor disponível e para bloquear novos puts até um get.
        System.out.println("Buffer recebeu (put): " + valor); // Imprime o valor que acabou de ser colocado no buffer (linha aparece no console).
        notifyAll(); // Acorda threads esperando (principalmente o consumidor) para ele poder continuar e fazer o próximo get.
    } // Fecha o método put; não imprime ao fechar, apenas delimita o fim do método.
} // Fecha a classe Buffer; não imprime nada, apenas encerra a definição do recurso compartilhado.
