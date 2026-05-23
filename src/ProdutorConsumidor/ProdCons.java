// Bloco de cabeçalho do trabalho (não imprime na tela; serve como documentação do código e dos comandos).
/*
* TRABALHO DE SISTEMAS OPERACIONAIS - PARTE I (THREADS) - JAVA
* PROBLEMA DO PRODUTOR E CONSUMIDOR (BOUNDED BUFFER PROBLEM)
*
* INTEGRANTES DO GRUPO:
* - Rafael Lopes
* - Cleverson Resende
* - Matheus Barbosa
* - Bernado Melgaço
*
* ENTRADA:
* Este programa não recebe entrada do teclado. A execução cria 1 produtor e 1 consumidor.
*
* COMO COMPILAR (NA RAIZ DO PROJETO):
* javac -d out src/ProdutorConsumidor/*.java
*
* COMO EXECUTAR (NA RAIZ DO PROJETO):
* java -cp out ProdutorConsumidor.ProdCons
*/

package ProdutorConsumidor; // Define o pacote para organizar as classes e permitir executar como ProdutorConsumidor.ProdCons.
// Este arquivo é o ponto de entrada do exemplo do Produtor-Consumidor (não imprime nada sozinho nesta linha).
public class ProdCons { // Declara a classe principal que contém o main, responsável por criar as threads.
    public static void main(String[] args) { // Método principal chamado ao executar o programa; aqui começa a saída no console.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Bernado Melgaço."); // Imprime os nomes do grupo (primeira linha exibida na tela).
        Buffer buf = new Buffer(); // Cria o buffer compartilhado (recurso disputado); não imprime aqui, mas influencia os prints do put/get.
        int i; // Declara uma variável para identificar o par produtor/consumidor criado; não imprime nada.
        for (i = 0; i < 1; i++) { // Loop para criar 1 par (i=0) de threads; não imprime, mas inicia concorrência que gera prints.
            new Consumidor(buf, i).start(); // Inicia o consumidor; ele imprimirá quando fizer get e quando confirmar a remoção do valor.
            new Produtor(buf, i).start(); // Inicia o produtor; ele imprimirá quando fizer put e quando confirmar a inserção do valor.
        } // Fecha o loop; após iniciar as threads, o main termina sem esperar, mas o processo continua enquanto as threads rodam.
    } // Fecha o método main; não imprime nada ao fechar, apenas delimita o fim do método.
} // Fecha a classe ProdCons; não imprime nada, apenas encerra a definição da classe.
