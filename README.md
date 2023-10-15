# simple java build tool (SJBT)
Prova de Conceito de uma ferramenta capaz de compilar e instalar dependências automaticamente para criar um fat/uber jar em um projeto em Java.

## Compilação
Para compilar o projeto é necessário usar a própia ferramenta para a compilação
```
javac build.java
java build build.java
```
Resulta em um ```build.jar``` que poderá ser adicionado a qualquer projeto. Esse repositório ja inclui um por convêniencia.

É perfeitamente possível utilizar a ferramenta para compilar a si mesmo:
```
java -jar build.jar build.java
```
SBJT sempre compila um projeto para um ```.jar```.

Idealmente projetos que usassem o SJBT teriam esse ```build.jar``` diretamente dentro do projeto. Isso difere de outras ferramentas de 
compilação como o ```gcc``` ou ```npm``` que necessitam ser instaladas no sistema do usuário. 

## Gerenciamento de Dependências
SJBT não seria uma ferramenta completa se não fosse capaz de gerenciar dependências.
dependências são declaradas dentro do arquivo principal como comentarios
```java
// SJBT: @dependency org.jsoup:jsoup:1.16.1
```
Links para .zip contendo todas as classes da biblioteca também funcionam, porém SBJT é perfeitamente capaz de utilizar a notação padrão
do maven. SBJT sempre irá incluir todas as dependências no ```.jar``` final (AKA uber jar).

Um exemplo completo:
```java
// SJBT: @dependency org.jsoup:jsoup:1.16.1

import org.jsoup.Jsoup;

class Test {
    public static void main(String[] args) {
        System.out.println("foo bar foo bar");
        try {
            var doc = Jsoup.connect("https://example.com").get();
            System.out.println(doc.title());
            var newsHeadlines = doc.select("#mp-itn b a");
            for (var headline : newsHeadlines) {
                System.out.printf("%s\n\t%s", 
                        headline.attr("title"), headline.absUrl("href"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
Alem do mais, nenhum arquivo de configuração é necessário.

## Caveats
- É bem capaz que seu LSP não funcione corretamente (a ferramenta do seu editor que te da sugestões de texto).
- Talvez não funcione em projetos onde a estrutura dos arquivos é um pouco complexa.
- Gera uma pasta "build" no seu diretóirio principal.

## Racional
Tentei usar o maven para um projeto bem simples e não gostei do que vi.

