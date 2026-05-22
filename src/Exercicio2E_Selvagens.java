/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(e) - O JANTAR DOS SELVAGENS
 *
 * INTEGRANTES DO GRUPO:
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Bernado Melgaço
 *
 * ENTRADA - PARÂMETROS DA SIMULAÇÃO:
 * 1) Capacidade do caldeirão (N porções quando cheio)
 * 2) Quantidade de selvagens (threads)
 * 3) Quantidade de porções que cada selvagem comerá
 * 4) Tempo mínimo para comer (ms)
 * 5) Tempo máximo para comer (ms)
 *
 * ENTRADA CASOS DE TESTE:
 * Caso 1 (força acordar o cozinheiro com frequência):
 * N=1, selvagens=3, porcoesPorSelvagem=3, comerMin=50, comerMax=150
 *
 * Caso 2 (médio):
 * N=5, selvagens=5, porcoesPorSelvagem=5, comerMin=50, comerMax=200
 *
 * Caso 3 (dificil):
 * N=20, selvagens=30, porcoesPorSelvagem=10, comerMin=10, comerMax=80
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2E_Selvagens.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2E_Selvagens
 */
import java.util.Random; // Importa Random para sortear tempos de comer e simular concorrência com atrasos variáveis.
import java.util.Scanner; // Importa Scanner para ler os parâmetros digitados no teclado.
import java.util.concurrent.Semaphore; // Importa Semaphore para sincronizar selvagens e cozinheiro sem busy wait.

public class Exercicio2E_Selvagens { // Declara a classe principal do exercício com método main para execução direta.

    private static final class Caldeirao { // Declara uma classe interna para guardar o estado do caldeirão e os semáforos de sincronização.

        private final int capacidade; // Guarda quantas porções o caldeirão tem quando está cheio.

        private final Semaphore mutexServir; // Garante que apenas 1 selvagem se sirva por vez, atendendo a restrição de sincronização.
        private final Semaphore caldeiraoVazio; // Usado para acordar o cozinheiro quando o caldeirão fica vazio.
        private final Semaphore caldeiraoCheio; // Usado para liberar o selvagem que esperou o cozinheiro encher o caldeirão.

        private int porcoes; // Guarda quantas porções estão atualmente disponíveis no caldeirão.

        private int totalServidas; // Contador total de porções servidas, para imprimir um resumo ao final.
        private int totalReabastecimentos; // Contador de quantas vezes o cozinheiro encheu o caldeirão, para imprimir resumo ao final.

        private Caldeirao(int capacidade) { // Construtor do caldeirão que inicializa semáforos e porções iniciais.
            this.capacidade = capacidade; // Armazena a capacidade informada pelo usuário.
            this.mutexServir = new Semaphore(1, true); // Começa liberado para permitir que o primeiro selvagem se sirva.
            this.caldeiraoVazio = new Semaphore(0, true); // Começa em 0 porque o cozinheiro dorme até alguém acordá-lo.
            this.caldeiraoCheio = new Semaphore(0, true); // Começa em 0 porque só libera quando o cozinheiro encher.
            this.porcoes = capacidade; // Inicia o caldeirão cheio para começar a simulação com porções disponíveis.
            this.totalServidas = 0; // Inicializa contador total de porções servidas como 0.
            this.totalReabastecimentos = 0; // Inicializa contador total de reabastecimentos como 0.
        }

        private void servir(String nomeSelvagem) throws InterruptedException { // Método chamado por um selvagem para pegar 1 porção (ou acordar cozinheiro).
            mutexServir.acquire(); // Entra na região crítica de "servir", garantindo que apenas 1 selvagem mexe no caldeirão por vez.
            if (porcoes == 0) { // Verifica se o caldeirão está vazio antes de se servir.
                System.out.println(nomeSelvagem + " encontrou o caldeirão vazio e vai acordar o cozinheiro."); // Imprime que não há porções e que o cozinheiro será acordado.
                caldeiraoVazio.release(); // Acorda o cozinheiro para ele reabastecer (sinaliza que o caldeirão está vazio).
                System.out.println(nomeSelvagem + " está aguardando o cozinheiro encher o caldeirão."); // Imprime que o selvagem vai aguardar o reabastecimento.
                mutexServir.release(); // Libera o mutex para permitir que o cozinheiro reabasteça sem ficar bloqueado.
                caldeiraoCheio.acquire(); // Aguarda o cozinheiro encher o caldeirão e liberar este semáforo.
                mutexServir.acquire(); // Reentra no mutex para efetivamente se servir, mantendo a regra de 1 por vez.
            }
            porcoes--; // Decrementa 1 porção, representando que o selvagem se serviu.
            totalServidas++; // Incrementa o total de porções servidas, para resumo no final.
            System.out.println(nomeSelvagem + " se serviu de 1 porção. Porções restantes no caldeirão: " + porcoes + "."); // Imprime o resultado do ato de se servir e o que sobrou.
            mutexServir.release(); // Sai da região crítica, permitindo que outro selvagem tente se servir.
        }

        private void reabastecer(String nomeCozinheiro) throws InterruptedException { // Método chamado pelo cozinheiro quando acordado para encher o caldeirão.
            caldeiraoVazio.acquire(); // Bloqueia enquanto ninguém acordar o cozinheiro; quando liberar, significa que o caldeirão está vazio.
            mutexServir.acquire(); // Entra no mutex para garantir que a atualização de "porcoes" seja feita sem interferência.
            if (porcoes == 0) { // Confirma que o caldeirão realmente está vazio, respeitando a restrição do enunciado.
                porcoes = capacidade; // Enche o caldeirão com a capacidade total de porções.
                totalReabastecimentos++; // Incrementa o contador de reabastecimentos para imprimir um resumo no final.
                System.out.println(nomeCozinheiro + " encheu o caldeirão com " + capacidade + " porções."); // Imprime que o caldeirão foi reabastecido.
            } else { // Se por algum motivo ele foi acordado com caldeirão não vazio, imprime situação inesperada para depuração.
                System.out.println(nomeCozinheiro + " foi acordado, mas o caldeirão não estava vazio (isso não deveria acontecer)."); // Imprime mensagem de alerta.
            }
            mutexServir.release(); // Sai do mutex, liberando o acesso de servir novamente.
            caldeiraoCheio.release(); // Libera 1 selvagem que estava aguardando o caldeirão ser reabastecido.
        }
    }

    private static final class Cozinheiro extends Thread { // Declara a thread do cozinheiro, que dorme até ser acordado para reabastecer.

        private final Caldeirao caldeirao; // Referência ao caldeirão compartilhado para reabastecer porções e sincronizar.
        private final int totalReabastecimentosNecessarios; // Guarda quantas vezes o cozinheiro precisará encher para atender toda a simulação.

        private Cozinheiro(Caldeirao caldeirao, int totalReabastecimentosNecessarios) { // Construtor do cozinheiro com referência e limite de reabastecimentos.
            this.caldeirao = caldeirao; // Armazena o caldeirão para usar semáforos e atualizar porções.
            this.totalReabastecimentosNecessarios = totalReabastecimentosNecessarios; // Armazena quantas vezes ele deverá reabastecer para poder encerrar.
            setName("Cozinheiro"); // Define o nome da thread para facilitar identificação nos prints.
        }

        @Override
        public void run() { // Executa o loop do cozinheiro para reabastecer sempre que necessário.
            String nome = getName(); // Obtém o nome da thread para prefixar mensagens.
            try { // Inicia bloco try para tratar InterruptedException.
                while (caldeirao.totalReabastecimentos < totalReabastecimentosNecessarios) { // Enquanto ainda houver reabastecimentos previstos, continua dormindo e reabastecendo.
                    System.out.println(nome + " está dormindo aguardando algum selvagem acordá-lo (caldeirão vazio)."); // Imprime que o cozinheiro está em espera.
                    caldeirao.reabastecer(nome); // Executa 1 reabastecimento quando um selvagem sinalizar caldeirão vazio.
                    System.out.println(nome + " voltou a dormir após reabastecer."); // Imprime que ele termina a ação e volta ao estado de espera.
                }
                System.out.println(nome + " concluiu todos os reabastecimentos necessários e encerrará sua thread."); // Imprime que o cozinheiro finaliza porque não há mais demanda.
            } catch (InterruptedException e) { // Captura interrupção caso o programa interrompa o cozinheiro.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que o cozinheiro foi interrompido.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção como boa prática.
            }
        }
    }

    private static final class Selvagem extends Thread { // Declara a thread de um selvagem que repetidamente se serve e come.

        private final int id; // Identificador do selvagem para diferenciar suas mensagens.
        private final int porcoesParaComer; // Quantidade de porções que este selvagem deve comer até encerrar.
        private final Caldeirao caldeirao; // Referência ao caldeirão para se servir com sincronização.
        private final int comerMinMs; // Tempo mínimo para comer, usado para sorteio.
        private final int comerMaxMs; // Tempo máximo para comer, usado para sorteio.
        private final Random random; // Random para sortear tempo de comer.

        private Selvagem(int id, int porcoesParaComer, Caldeirao caldeirao, int comerMinMs, int comerMaxMs, Random random) { // Construtor do selvagem com seus parâmetros.
            this.id = id; // Armazena o id do selvagem.
            this.porcoesParaComer = porcoesParaComer; // Armazena quantas porções ele comerá, limitando a simulação.
            this.caldeirao = caldeirao; // Armazena a referência ao caldeirão compartilhado.
            this.comerMinMs = comerMinMs; // Armazena o mínimo para sortear o tempo de comer.
            this.comerMaxMs = comerMaxMs; // Armazena o máximo para sortear o tempo de comer.
            this.random = random; // Armazena o Random para sorteio dos tempos.
            setName("Selvagem-" + id); // Define o nome da thread para facilitar identificar nos prints.
        }

        private int sortearTempoComer() { // Método auxiliar para sortear um tempo de comer no intervalo definido.
            if (comerMaxMs <= comerMinMs) { // Se max <= min, não há intervalo; usa min.
                return comerMinMs; // Retorna o mínimo para evitar erro e manter previsibilidade.
            }
            return comerMinMs + random.nextInt((comerMaxMs - comerMinMs) + 1); // Retorna um tempo aleatório entre min e max (inclusive).
        }

        @Override
        public void run() { // Executa o comportamento do selvagem (servir e comer) várias vezes.
            String nome = getName(); // Obtém o nome da thread para prefixar mensagens.
            try { // Inicia bloco try para tratar InterruptedException.
                for (int i = 1; i <= porcoesParaComer; i++) { // Loop para comer o número de porções definido pelo usuário.
                    System.out.println(nome + " quer comer a porção " + i + " de " + porcoesParaComer + " e vai tentar se servir."); // Imprime intenção de comer e que vai se servir.
                    caldeirao.servir(nome); // Tenta se servir (pode acordar cozinheiro e esperar se estiver vazio).
                    int tempo = sortearTempoComer(); // Sorteia quanto tempo vai comer, simulando que comer pode ocorrer em paralelo.
                    System.out.println(nome + " começou a comer por " + tempo + " ms (comer pode ocorrer em paralelo com outros selvagens)."); // Imprime que está comendo e ressalta que comer é paralelo.
                    if (tempo > 0) { // Verifica se o tempo é positivo para evitar sleep desnecessário.
                        Thread.sleep(tempo); // Dorme para simular o ato de comer enquanto outros selvagens podem comer simultaneamente.
                    }
                    System.out.println(nome + " terminou de comer a porção " + i + "."); // Imprime que terminou de comer a porção atual.
                }
                System.out.println(nome + " comeu todas as suas porções e encerrará sua thread."); // Imprime que este selvagem encerra após terminar suas porções.
            } catch (InterruptedException e) { // Captura interrupção caso o programa interrompa o selvagem.
                System.out.println(nome + " foi interrompido e encerrará."); // Imprime que foi interrompido.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção como boa prática.
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) { // Método utilitário para ler um inteiro com validação para evitar erros por entrada inválida.
        System.out.print(mensagem); // Imprime a mensagem para orientar o usuário sobre o que digitar.
        while (!scanner.hasNextInt()) { // Enquanto a entrada não for um inteiro válido, continua pedindo.
            System.out.println("Valor inválido. Digite um número inteiro."); // Imprime aviso de erro e pede correção.
            scanner.next(); // Descarta o token inválido para tentar ler novamente.
            System.out.print(mensagem); // Reimprime a mensagem original para solicitar novamente.
        }
        return scanner.nextInt(); // Retorna o inteiro válido digitado pelo usuário.
    }

    private static int calcularReabastecimentosNecessarios(int capacidade, int totalPorcoesASeremComidas) { // Calcula quantas vezes o cozinheiro precisará encher o caldeirão.
        if (capacidade <= 0) { // Se a capacidade for inválida (<=0), evita divisão por zero e retorna 0 para não travar.
            return 0; // Retorna 0 para indicar que não faz sentido reabastecer com capacidade inválida.
        }
        int porcoesIniciais = capacidade; // Considera que o caldeirão inicia cheio com "capacidade" porções.
        int porcoesRestantesDepoisDoInicio = totalPorcoesASeremComidas - porcoesIniciais; // Calcula quanto ainda precisa ser comido após consumir o conteúdo inicial.
        if (porcoesRestantesDepoisDoInicio <= 0) { // Se o total cabe no caldeirão inicial, nenhum reabastecimento é necessário.
            return 0; // Retorna 0 porque o cozinheiro não precisará encher nenhuma vez.
        }
        int div = porcoesRestantesDepoisDoInicio / capacidade; // Calcula quantos caldeirões cheios cabem nas porções restantes.
        int mod = porcoesRestantesDepoisDoInicio % capacidade; // Calcula o resto para saber se precisa de mais um reabastecimento parcial.
        return (mod == 0) ? div : (div + 1); // Retorna o número de reabastecimentos: exato se mod=0, senão mais 1 para cobrir o resto.
    }

    public static void main(String[] args) throws Exception { // Declara o main para ler entrada, iniciar threads e finalizar a simulação.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Gustavo Cicero, Bernado Melgaço."); // Imprime os integrantes conforme exigido.
        System.out.println("Exercício 2(e) - O jantar dos Selvagens (Threads + Semáforos)."); // Imprime o título do exercício para contextualizar.
        Scanner scanner = new Scanner(System.in); // Cria Scanner para ler valores do teclado.
        int capacidade = lerInteiro(scanner, "Digite a capacidade do caldeirão (N porções quando cheio): "); // Lê a capacidade do caldeirão.
        int qtdSelvagens = lerInteiro(scanner, "Digite a quantidade de selvagens: "); // Lê a quantidade de threads de selvagens.
        int porcoesPorSelvagem = lerInteiro(scanner, "Digite quantas porções cada selvagem comerá: "); // Lê quantas porções cada selvagem comerá.
        int comerMin = lerInteiro(scanner, "Digite o tempo mínimo para comer (ms): "); // Lê o tempo mínimo para comer.
        int comerMax = lerInteiro(scanner, "Digite o tempo máximo para comer (ms): "); // Lê o tempo máximo para comer.
        int totalPorcoes = qtdSelvagens * porcoesPorSelvagem; // Calcula quantas porções totais serão consumidas pela tribo durante a simulação.
        int reabastecimentosNecessarios = calcularReabastecimentosNecessarios(capacidade, totalPorcoes); // Calcula quantas vezes o cozinheiro precisará encher o caldeirão.
        System.out.println("Total de porções que serão consumidas: " + totalPorcoes + "."); // Imprime o total de porções para o usuário conferir o cenário.
        System.out.println("Reabastecimentos necessários (considerando caldeirão inicia cheio): " + reabastecimentosNecessarios + "."); // Imprime quantas vezes o cozinheiro deverá ser acordado.
        Random random = new Random(); // Cria um Random para sortear tempos de comer.
        Caldeirao caldeirao = new Caldeirao(capacidade); // Cria o caldeirão compartilhado com capacidade e semáforos.
        Cozinheiro cozinheiro = new Cozinheiro(caldeirao, reabastecimentosNecessarios); // Cria o cozinheiro com o número de reabastecimentos necessários para encerrar corretamente.
        cozinheiro.start(); // Inicia a thread do cozinheiro (ele dormirá esperando ser acordado pelos selvagens).
        Thread[] selvagens = new Thread[qtdSelvagens]; // Cria um array para guardar threads dos selvagens e aguardar no final.
        for (int i = 1; i <= qtdSelvagens; i++) { // Loop para criar todos os selvagens com ids de 1 a qtdSelvagens.
            selvagens[i - 1] = new Selvagem(i, porcoesPorSelvagem, caldeirao, comerMin, comerMax, random); // Cria o selvagem com id, quantidade de porções e parâmetros de comer.
            selvagens[i - 1].start(); // Inicia a thread do selvagem para ele começar a se servir e comer concorrentemente.
        }
        for (Thread s : selvagens) { // Percorre o array de threads dos selvagens para aguardar todas terminarem.
            s.join(); // Espera cada selvagem encerrar após comer todas as suas porções.
        }
        System.out.println("Todos os selvagens terminaram. Aguardando o cozinheiro encerrar."); // Imprime que os selvagens acabaram e que falta o cozinheiro finalizar.
        if (reabastecimentosNecessarios == 0) { // Se nenhum reabastecimento foi necessário, o cozinheiro pode estar dormindo bloqueado em caldeiraoVazio.
            System.out.println("Como não há reabastecimentos necessários, interrompendo o cozinheiro para encerrar sem travar."); // Imprime que vamos interromper para não ficar preso em acquire.
            cozinheiro.interrupt(); // Interrompe o cozinheiro para ele sair do acquire e encerrar.
        }
        cozinheiro.join(); // Aguarda o cozinheiro encerrar, garantindo finalização limpa do programa.
        System.out.println("Resumo final: porções servidas = " + caldeirao.totalServidas + ", reabastecimentos = " + caldeirao.totalReabastecimentos + "."); // Imprime resumo final para validar os resultados.
        scanner.close(); // Fecha o Scanner para liberar o recurso de entrada padrão.
    }
}

