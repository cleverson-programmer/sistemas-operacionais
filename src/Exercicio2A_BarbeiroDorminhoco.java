/*
 * TRABALHO DE SISTEMAS OPERACIONAIS - PARTE II (THREADS)
 * EXERCÍCIO 2(a) - PROBLEMA DO BARBEIRO DORMINHOCO
 *
 * INTEGRANTES DO GRUPO (IMPRIMIR EM TODOS OS PROGRAMAS):
 * - Rafael Lopes
 * - Cleverson Resende
 * - Matheus Barbosa
 * - Gustavo Cicero
 * - Bernado Melgaço
 *
 * OBJETIVO:
 * Simular uma barbearia com 1 barbeiro, 1 cadeira de barbeiro e N cadeiras de espera.
 * Usar threads para simular concorrência e semáforos para evitar condições de corrida.
 *
 * ENTRADA (DIGITADA NO TECLADO) - PARÂMETROS DA SIMULAÇÃO:
 * 1) Número de cadeiras de espera (N)
 * 2) Quantidade de clientes a serem gerados
 * 3) Tempo mínimo do corte (ms)
 * 4) Tempo máximo do corte (ms)
 * 5) Tempo mínimo entre chegadas de clientes (ms)
 * 6) Tempo máximo entre chegadas de clientes (ms)
 *
 * ENTRADA (CASOS DE TESTE SUGERIDOS):
 * Caso 1 (pequeno):
 * N=1, clientes=5, corteMin=200, corteMax=400, chegadaMin=50, chegadaMax=100
 *
 * Caso 2 (médio):
 * N=3, clientes=20, corteMin=200, corteMax=800, chegadaMin=50, chegadaMax=300
 *
 * Caso 3 (estresse / filas cheias):
 * N=0, clientes=10, corteMin=200, corteMax=800, chegadaMin=50, chegadaMax=150
 *
 * COMO COMPILAR (NA RAIZ DO PROJETO):
 * javac -d out src/Exercicio2A_BarbeiroDorminhoco.java
 *
 * COMO EXECUTAR (NA RAIZ DO PROJETO):
 * java -cp out Exercicio2A_BarbeiroDorminhoco
 */
import java.util.ArrayList; // Importa ArrayList para guardar as threads de clientes e depois aguardar (join).
import java.util.List; // Importa List para trabalhar com a lista de clientes de forma genérica.
import java.util.Random; // Importa Random para gerar tempos aleatórios de chegada e de corte.
import java.util.Scanner; // Importa Scanner para ler os valores de entrada digitados no teclado.
import java.util.concurrent.Semaphore; // Importa Semaphore para sincronização entre threads sem busy wait.

public class Exercicio2A_BarbeiroDorminhoco { // Declara a classe principal (sem package) para facilitar compilar/rodar com javac/java.

    private static final class Barbearia { // Declara uma classe interna para centralizar todo o estado compartilhado da barbearia.

        private final int numeroCadeirasDeEspera; // Guarda quantas cadeiras de espera existem na barbearia.
        private final Random random; // Guarda o gerador de aleatoriedade usado pelo barbeiro para simular tempo de corte.

        private final int tempoCorteMinMs; // Guarda o tempo mínimo (em ms) que um corte pode durar.
        private final int tempoCorteMaxMs; // Guarda o tempo máximo (em ms) que um corte pode durar.

        private final Semaphore customers; // Semáforo que conta quantos clientes estão esperando atendimento.
        private final Semaphore barbers; // Semáforo que indica quando o barbeiro está pronto para atender 1 cliente.
        private final Semaphore mutex; // Semáforo de exclusão mútua para proteger variáveis compartilhadas (região crítica).

        private final Semaphore clienteNaCadeira; // Semáforo para o cliente avisar que sentou na cadeira de barbeiro.
        private final Semaphore corteFinalizado; // Semáforo para o barbeiro avisar que terminou o corte do cliente atual.

        private int waiting; // Variável compartilhada que conta clientes esperando (cópia do semáforo customers, como no enunciado).
        private int clientesAtendidos; // Contador para imprimir ao final quantos clientes foram atendidos.
        private int clientesForamEmbora; // Contador para imprimir ao final quantos clientes foram embora por falta de cadeira.

        private boolean barbeariaAberta; // Flag para indicar se ainda podem chegar clientes (usada para encerrar o barbeiro sem travar).

        private Barbearia(int numeroCadeirasDeEspera, int tempoCorteMinMs, int tempoCorteMaxMs, Random random) { // Construtor da barbearia com parâmetros da simulação.
            this.numeroCadeirasDeEspera = numeroCadeirasDeEspera; // Armazena o número de cadeiras de espera definido pelo usuário.
            this.tempoCorteMinMs = tempoCorteMinMs; // Armazena o tempo mínimo do corte definido pelo usuário.
            this.tempoCorteMaxMs = tempoCorteMaxMs; // Armazena o tempo máximo do corte definido pelo usuário.
            this.random = random; // Armazena o Random para gerar durações de corte.
            this.customers = new Semaphore(0); // Inicialmente não há clientes esperando, então começa em 0.
            this.barbers = new Semaphore(0); // Inicialmente o barbeiro não está chamando ninguém, então começa em 0.
            this.mutex = new Semaphore(1); // Mutex começa liberado (1) para permitir entrada na região crítica.
            this.clienteNaCadeira = new Semaphore(0); // Começa em 0 porque nenhum cliente está na cadeira de barbeiro ainda.
            this.corteFinalizado = new Semaphore(0); // Começa em 0 porque nenhum corte foi finalizado ainda.
            this.waiting = 0; // No início, ninguém está esperando.
            this.clientesAtendidos = 0; // No início, nenhum cliente foi atendido.
            this.clientesForamEmbora = 0; // No início, nenhum cliente foi embora.
            this.barbeariaAberta = true; // No início, a barbearia está aberta para receber clientes.
        }

        private void fecharBarbearia() throws InterruptedException { // Método chamado pela thread principal quando todos os clientes já foram gerados e finalizaram.
            mutex.acquire(); // Entra na região crítica para alterar a flag com segurança.
            barbeariaAberta = false; // Define que a barbearia não aceita mais clientes (apenas finaliza os que já estão no fluxo).
            mutex.release(); // Sai da região crítica liberando o mutex para outras threads.
            customers.release(); // Libera 1 permissão para acordar o barbeiro caso ele esteja dormindo bloqueado em customers.acquire().
        }

        private boolean tentarEntrarNaEspera(int idCliente) throws InterruptedException { // Método executado por um cliente ao chegar para decidir se espera ou vai embora.
            mutex.acquire(); // Entra na região crítica para ler/alterar "waiting" com exclusão mútua.
            if (waiting >= numeroCadeirasDeEspera) { // Verifica se já não há mais cadeiras disponíveis para esperar.
                clientesForamEmbora++; // Incrementa o contador de clientes que vão embora (serve para imprimir no final).
                System.out.println("Cliente " + idCliente + " chegou, mas não há cadeira de espera. Cliente vai embora sem cortar o cabelo."); // Imprime o motivo do cliente ir embora.
                mutex.release(); // Libera o mutex para permitir que outras threads acessem a região crítica.
                return false; // Retorna falso para indicar que este cliente não será atendido.
            }
            waiting++; // Incrementa o número de clientes esperando (mantendo a cópia do semáforo customers).
            System.out.println("Cliente " + idCliente + " chegou e sentou na espera. Clientes esperando agora: " + waiting + "."); // Imprime que o cliente entrou na fila de espera.
            customers.release(); // Faz um "up" em customers para avisar ao barbeiro que existe cliente esperando (acordando-o se estiver dormindo).
            mutex.release(); // Sai da região crítica liberando o mutex.
            return true; // Retorna verdadeiro para indicar que o cliente vai esperar e ser atendido quando for chamado.
        }

        private void registrarAtendimentoConcluido(int idCliente) throws InterruptedException { // Método para contabilizar que o cliente foi efetivamente atendido.
            mutex.acquire(); // Entra na região crítica para alterar o contador de atendidos com segurança.
            clientesAtendidos++; // Soma 1 no total de atendidos para imprimir o resumo ao final.
            System.out.println("Cliente " + idCliente + " terminou o corte e saiu da barbearia."); // Imprime que o cliente finalizou e saiu.
            mutex.release(); // Sai da região crítica liberando o mutex.
        }

        private int sortearTempoCorteMs() { // Método auxiliar para sortear um tempo de corte dentro do intervalo definido.
            if (tempoCorteMaxMs <= tempoCorteMinMs) { // Se o usuário informar max <= min, usa o mínimo para evitar erro.
                return tempoCorteMinMs; // Retorna o tempo mínimo como valor fixo do corte.
            }
            return tempoCorteMinMs + random.nextInt((tempoCorteMaxMs - tempoCorteMinMs) + 1); // Retorna um inteiro aleatório entre min e max (inclusive).
        }
    }

    private static final class Barbeiro extends Thread { // Declara a thread do barbeiro, que fica em loop atendendo clientes.

        private final Barbearia barbearia; // Guarda a referência para a barbearia (estado compartilhado).

        private Barbeiro(Barbearia barbearia) { // Construtor que recebe a barbearia.
            this.barbearia = barbearia; // Armazena a referência da barbearia para usar os semáforos e variáveis.
            setName("Barbeiro"); // Define o nome da thread para facilitar leitura das mensagens (não imprime sozinho, mas ajuda em debug).
        }

        @Override
        public void run() { // Método que executa o comportamento do barbeiro.
            try { // Inicia bloco try para capturar InterruptedException sem derrubar a aplicação sem mensagem.
                while (true) { // Loop infinito: o barbeiro tenta atender clientes repetidamente até a barbearia fechar e não haver espera.
                    System.out.println("Barbeiro está esperando clientes (se não houver, ele dorme bloqueado no semáforo)."); // Imprime que o barbeiro está pronto e pode dormir se não houver clientes.
                    barbearia.customers.acquire(); // Bloqueia até existir pelo menos 1 cliente esperando (ou até a thread principal liberar ao fechar).
                    barbearia.mutex.acquire(); // Entra na região crítica para verificar com segurança se há clientes esperando e/ou se a barbearia fechou.
                    if (!barbearia.barbeariaAberta && barbearia.waiting == 0) { // Se a barbearia fechou e não há ninguém esperando, então pode encerrar.
                        barbearia.mutex.release(); // Libera o mutex antes de sair do loop para não travar outras threads.
                        System.out.println("Barbeiro percebeu que a barbearia fechou e não há clientes esperando. Encerrando o expediente."); // Imprime que o barbeiro vai encerrar.
                        break; // Sai do loop para terminar a thread do barbeiro.
                    }
                    barbearia.waiting--; // Decrementa o número de clientes esperando, pois um deles vai sair da espera para ser atendido.
                    System.out.println("Barbeiro chamou um cliente. Clientes ainda esperando: " + barbearia.waiting + "."); // Imprime o estado atualizado da fila de espera.
                    barbearia.barbers.release(); // Sinaliza que o barbeiro está pronto, liberando exatamente 1 cliente que estava esperando em barbers.acquire().
                    barbearia.mutex.release(); // Sai da região crítica liberando o mutex.
                    barbearia.clienteNaCadeira.acquire(); // Espera o cliente liberado avisar que sentou na cadeira de barbeiro (garante ordem correta).
                    int tempoCorte = barbearia.sortearTempoCorteMs(); // Sorteia quanto tempo este corte vai durar, para simular trabalho do barbeiro.
                    System.out.println("Barbeiro começou a cortar o cabelo (duração: " + tempoCorte + " ms)."); // Imprime o início do corte e o tempo escolhido.
                    Thread.sleep(tempoCorte); // Dorme por "tempoCorte" para simular o tempo real do serviço sendo realizado.
                    System.out.println("Barbeiro terminou o corte de cabelo."); // Imprime que o corte terminou.
                    barbearia.corteFinalizado.release(); // Libera o cliente para ele poder finalizar e sair da barbearia.
                }
            } catch (InterruptedException e) { // Captura interrupção (ex.: se algo encerrar o programa e interromper a thread).
                System.out.println("Barbeiro foi interrompido e vai encerrar."); // Imprime que a thread do barbeiro foi interrompida.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção para boas práticas.
            }
        }
    }

    private static final class Cliente extends Thread { // Declara a thread de um cliente.

        private final int idCliente; // Guarda um identificador numérico do cliente para imprimir nas mensagens.
        private final Barbearia barbearia; // Guarda a referência para a barbearia (estado compartilhado).
        private final int atrasoChegadaMs; // Guarda quanto tempo este cliente vai demorar para chegar (para simular chegadas em momentos diferentes).

        private Cliente(int idCliente, Barbearia barbearia, int atrasoChegadaMs) { // Construtor que define o id, a barbearia e o atraso de chegada.
            this.idCliente = idCliente; // Armazena o identificador do cliente para uso nos prints.
            this.barbearia = barbearia; // Armazena a referência do estado compartilhado (semáforos e variáveis).
            this.atrasoChegadaMs = atrasoChegadaMs; // Armazena o atraso de chegada (ms) que será usado com sleep.
            setName("Cliente-" + idCliente); // Define um nome para a thread do cliente (útil para debug).
        }

        @Override
        public void run() { // Método que executa o comportamento do cliente.
            try { // Inicia bloco try para tratar InterruptedException.
                Thread.sleep(atrasoChegadaMs); // Espera um tempo antes de "chegar" na barbearia, simulando chegada em instantes diferentes.
                System.out.println("Cliente " + idCliente + " chegou na barbearia após " + atrasoChegadaMs + " ms."); // Imprime o momento de chegada do cliente.
                boolean vaiSerAtendido = barbearia.tentarEntrarNaEspera(idCliente); // Tenta entrar na espera; pode falhar se as cadeiras estiverem lotadas.
                if (!vaiSerAtendido) { // Se não conseguiu entrar (sem cadeira disponível), o cliente encerra sua execução.
                    return; // Finaliza a thread do cliente, pois ele foi embora sem atendimento.
                }
                System.out.println("Cliente " + idCliente + " está aguardando ser chamado pelo barbeiro."); // Imprime que o cliente entrou no estado de espera pelo atendimento.
                barbearia.barbers.acquire(); // Bloqueia até o barbeiro sinalizar que está pronto para atender 1 cliente.
                System.out.println("Cliente " + idCliente + " foi chamado e sentou na cadeira de barbeiro."); // Imprime que o cliente foi liberado da espera para a cadeira.
                barbearia.clienteNaCadeira.release(); // Avisa ao barbeiro que o cliente realmente sentou na cadeira (permite iniciar o corte).
                barbearia.corteFinalizado.acquire(); // Espera o barbeiro terminar o corte (sem isso, o cliente poderia "sair" antes do corte acabar).
                barbearia.registrarAtendimentoConcluido(idCliente); // Registra e imprime que o atendimento terminou e o cliente saiu.
            } catch (InterruptedException e) { // Captura interrupção da thread do cliente.
                System.out.println("Cliente " + idCliente + " foi interrompido e vai encerrar."); // Imprime que o cliente foi interrompido.
                Thread.currentThread().interrupt(); // Restaura o status de interrupção por boas práticas.
            }
        }
    }

    private static int lerInteiro(Scanner scanner, String mensagem) { // Método utilitário para ler um inteiro do teclado com uma mensagem clara.
        System.out.print(mensagem); // Imprime a mensagem para orientar o usuário sobre o que deve ser digitado.
        while (!scanner.hasNextInt()) { // Enquanto o que foi digitado não for um inteiro válido, continua pedindo novamente.
            System.out.println("Valor inválido. Digite um número inteiro."); // Imprime um aviso para o usuário corrigir a entrada.
            scanner.next(); // Descarta o token inválido para poder tentar ler novamente.
            System.out.print(mensagem); // Reimprime a mensagem para pedir o valor novamente.
        }
        return scanner.nextInt(); // Retorna o inteiro válido digitado pelo usuário.
    }

    private static int sortearEntre(Random random, int min, int max) { // Método auxiliar para sortear um inteiro entre min e max (inclusive).
        if (max <= min) { // Se max <= min, não há intervalo válido; usa min.
            return min; // Retorna min como valor fixo para evitar IllegalArgumentException no nextInt.
        }
        return min + random.nextInt((max - min) + 1); // Retorna valor aleatório no intervalo, inclusive.
    }

    public static void main(String[] args) throws Exception { // Método principal que lê entrada, cria threads e coordena a simulação.
        System.out.println("Integrantes do grupo: Rafael Lopes, Cleverson Resende, Matheus Barbosa, Gustavo Cicero, Bernado Melgaço."); // Imprime os nomes do grupo conforme exigido.
        System.out.println("Exercício 2(a) - Problema do Barbeiro Dorminhoco (Threads + Semáforos)."); // Imprime o título do exercício para o usuário saber o que está rodando.
        Scanner scanner = new Scanner(System.in); // Cria o Scanner para ler os valores digitados pelo teclado.
        int nCadeiras = lerInteiro(scanner, "Digite o número de cadeiras de espera (N): "); // Lê N (número de cadeiras) para controlar quantos clientes podem esperar.
        int qtdClientes = lerInteiro(scanner, "Digite a quantidade de clientes a gerar: "); // Lê quantos clientes serão criados como threads nesta simulação.
        int corteMin = lerInteiro(scanner, "Digite o tempo mínimo do corte (ms): "); // Lê o tempo mínimo do corte para simular duração do atendimento.
        int corteMax = lerInteiro(scanner, "Digite o tempo máximo do corte (ms): "); // Lê o tempo máximo do corte para simular variação de duração do atendimento.
        int chegadaMin = lerInteiro(scanner, "Digite o tempo mínimo entre chegadas de clientes (ms): "); // Lê o tempo mínimo de chegada para espaçar as chegadas.
        int chegadaMax = lerInteiro(scanner, "Digite o tempo máximo entre chegadas de clientes (ms): "); // Lê o tempo máximo de chegada para aleatorizar as chegadas.
        Random random = new Random(); // Cria o gerador de números aleatórios para sortear atrasos de chegada e tempos de corte.
        Barbearia barbearia = new Barbearia(nCadeiras, corteMin, corteMax, random); // Cria a barbearia com os parâmetros e semáforos necessários.
        Barbeiro barbeiro = new Barbeiro(barbearia); // Cria a thread do barbeiro que ficará atendendo enquanto houver clientes.
        barbeiro.start(); // Inicia a execução do barbeiro (ele poderá dormir até algum cliente liberar "customers").
        List<Thread> clientes = new ArrayList<>(); // Cria uma lista para guardar todas as threads de clientes e poder aguardar o término delas.
        for (int i = 1; i <= qtdClientes; i++) { // Loop para criar a quantidade de clientes informada.
            int atraso = sortearEntre(random, chegadaMin, chegadaMax); // Sorteia um atraso de chegada para este cliente (simula chegar em um momento diferente).
            Cliente cliente = new Cliente(i, barbearia, atraso); // Cria a thread do cliente com id e atraso de chegada.
            clientes.add(cliente); // Adiciona a thread do cliente na lista para futuramente dar join e esperar terminar.
            cliente.start(); // Inicia a thread do cliente, que chegará após seu atraso e tentará ser atendido.
        }
        for (Thread cliente : clientes) { // Loop para aguardar todas as threads de clientes terminarem.
            cliente.join(); // Espera o cliente finalizar (se foi atendido ou se foi embora).
        }
        System.out.println("Todos os clientes já finalizaram (atendidos ou foram embora). Iniciando fechamento da barbearia."); // Informa que não haverá mais chegadas e que vamos encerrar.
        barbearia.fecharBarbearia(); // Fecha a barbearia e acorda o barbeiro caso ele esteja dormindo sem clientes.
        barbeiro.join(); // Aguarda a thread do barbeiro encerrar o loop e finalizar seu expediente.
        System.out.println("Resumo final: clientes atendidos = " + barbearia.clientesAtendidos + ", clientes que foram embora = " + barbearia.clientesForamEmbora + "."); // Imprime o resumo final para validação dos casos de teste.
        scanner.close(); // Fecha o Scanner para liberar o recurso de entrada padrão (boa prática).
    }
}

