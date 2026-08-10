# EMS: Exercício Prático BL125/2025

O **EduQa-nos Exam Management System** (ou _EMS_) é uma plataforma desenvolvida para o EduQa-nos, organização administrada pelo Ministério da Deseducação da República das Bananas Portuguesas, que permite a gestão centralizada dos exames nacionais, com um modelo de correção cega e atómica.

Depois de toda a confusão com os exames nacionais este ano (perdas de ficheiros e falhas informáticas), o Governo decidiu contratar o Instituto Inferior Técnico para desenvolver o projeto de raiz. Infelizmente, os 50 bolseiros da DSI (Desastre dos Sistemas Informáticos) não são suficientes, pelo que ficou a cargo de apenas 1 bolseiro do DEI o desenvolvimento do sistema completo.

Para resolver o problema de forma definitiva, é necessário o desenvolvimento de um subsistema para este efeito: o **EMS: Exam Management System** (o seu exercício prático).

Este subsistema deve permitir a gestão de:

- **Pessoas**, incluindo, para cada uma, pelo menos o nome, email e o seu tipo no sistema (administrador, funcionário da escola, professor, aluno, ou outros tipos que considere relevantes);
- **Escolas**, incluindo nome, código e região;
- **Disciplinas**, incluindo nome e código (ex: MAT-A, FQA);
- **Exames**, o documento original submetido em formato PDF, associado a uma escola e a uma disciplina;
- **Perguntas (Fragmentos)**, as imagens recortadas do PDF original, associadas a uma disciplina, a uma cotação máxima e ao exame de origem;
- **Pedidos de Revisão**, com justificação, prazo limite e histórico de decisões.

## Estrutura e Regras de Gestão

- Os **Administradores** gerem as entidades globais: criam Escolas, adicionam Funcionários e Professores, e associam os Professores às Escolas e a Disciplinas específicas.
- Os **Funcionários da Escola** fazem a gestão local: carregam os exames digitalizados (PDFs) dos alunos da sua escola e gerem a disponibilização das provas.
- Os **Professores** são avaliadores focados na sua disciplina. Não têm acesso à identidade do aluno nem à totalidade do exame - apenas a fragmentos isolados de perguntas.
- Os **Alunos** acedem ao sistema para consultar as suas classificações, as provas corrigidas e submeter pedidos de revisão.

## Workflow de Avaliação

O sistema opera num fluxo de trabalho estrito, dividido em cinco fases.

### Fase 1: Digitalização e Separação
- Os Funcionários da Escola efetuam o upload do PDF completo do exame de um aluno.
- O sistema fornece uma UI onde o Funcionário visualiza o PDF e consegue "separar" o documento em múltiplas imagens.
- Cada imagem gerada corresponde a uma Pergunta e é guardada na base de dados com a indicação da disciplina, cotação e o exame a que pertence.

### Fase 2: Distribuição de Tarefas
- Enquanto a Fase 1 decorre, os professores não têm acesso a qualquer prova.
- Apenas após todos os exames estarem carregados e segmentados em perguntas, o Administrador "bloqueia" a submissão e inicia o processo de distribuição.
- O sistema atribui a cada Professor de uma Disciplina um número de imagens de perguntas provenientes de múltiplos exames (ex: 50 resoluções da Pergunta 1), garantindo uma distribuição equitativa da carga de trabalho.

### Fase 3: Correção Atómica e Agregação
- Os Professores avaliam as imagens atribuídas e inserem a classificação de cada fragmento.
- O exame de um aluno transita para o estado "Corrigido" apenas quando todos os professores que receberam fragmentos dessa prova concluírem a sua avaliação.
- Concluídas as correções, o sistema calcula a nota final. As Escolas podem então gerar uma tabela com as classificações finais dos alunos da sua instituição, por disciplina.

### Fase 4: Disponibilização e Consulta
- Os Funcionários da Escola podem aprovar pedidos individuais de alunos para visualizar a prova, ou utilizar uma funcionalidade de **Bulk Release** (disponibilizar em lote o acesso às provas para todos os alunos de um determinado exame).
- Uma vez com acesso concedido, o aluno visualiza o PDF original lado a lado com as classificações obtidas em cada fragmento.

### Fase 5: Revisão de Prova
- Após a disponibilização das notas, inicia-se uma timeline rigorosa para pedidos de revisão (ex: 48 horas).
- Durante este período, o aluno pode selecionar perguntas específicas do seu exame e submeter um pedido de revisão com a respetiva justificação.
- **Fecho da Timeline**: quando o prazo termina, o sistema recolhe todos os fragmentos de perguntas alvo de pedido de revisão.
- Estes fragmentos são novamente distribuídos (idealmente a professores diferentes), seguindo a mesma lógica atómica da Fase 2: os professores recebem um lote de imagens isoladas correspondentes aos pedidos de revisão, avaliam a justificação e atribuem a nota final definitiva.

## Implementação Obrigatória

O objetivo deste exercício é desenvolver o subsistema **EMS** usando a framework de Backend e Frontend da sua preferência (ex: [Spring Boot](https://spring.io/projects/spring-boot)/Express/Django e [Vue.js](https://vuejs.org/)/React/Angular). Note que o código base foi desenvolvido em [Spring Boot](https://spring.io/projects/spring-boot) e [Vue.js](https://vuejs.org/).

O subsistema **EMS** deverá permitir, pelo menos:

- Gestão Global: CRUD de Escolas, Disciplinas e Utilizadores (apenas por Administradores);
- Associação de Professores a Escolas e Disciplinas (apenas por Administradores);
- Upload de ficheiros PDF de exames pelos Funcionários da Escola;
- Uma UI que permita visualizar o PDF e extrair páginas como imagens separadas (segmentação em perguntas);
- Implementar a lógica que distribui as imagens das perguntas de forma equilibrada pelos professores da disciplina correspondente, tanto na fase inicial como na fase de revisão de provas;
- Listagem de tarefas do Professor para visualizar as imagens atribuídas e submeter a classificação;
- Visualização de uma tabela filtrável, por parte dos Funcionários, com as notas finais dos alunos da sua escola, por disciplina;
- Sistema de aprovação de consulta de prova (pedidos individuais) e funcionalidade de Bulk Release;
- Implementação da funcionalidade que permite ao aluno pedir revisão (dentro de um prazo) e o respetivo re-encaminhamento desses fragmentos para correção;
- Autenticação e controlo de acessos (RBAC - Role Based Access Control) estrito para garantir o isolamento de privilégios entre Alunos, Professores, Funcionários e Administradores.

A solução que cumpra, com estabilidade, os requisitos descritos acima será avaliada com a **cotação máxima de 14 valores**.

**Nota:** Quando alguma situação ou detalhe não estiver explicitamente descrito neste enunciado, deve ser usado o **bom senso** e tomar decisões consistentes com a lógica de um sistema de gestão académica, documentando-as no ficheiro `README`.

### Funcionalidades Adicionais para Notas Superiores

Para alcançar uma **nota até 17 valores**, deve implementar **duas** das seguintes funcionalidades adicionais:

- **Cropping Interativo (Bounding Boxes)**: na UI de segmentação, em vez de separar páginas inteiras, o funcionário pode desenhar caixas sobre o PDF para recortar com precisão a área da resposta, gerando a imagem a partir desse recorte.
- **Correção com Anotações (Canvas)**: o professor, ao avaliar a imagem, dispõe de uma ferramenta de desenho (canvas) para adicionar anotações visuais (vistos, correções, texto) diretamente sobre a imagem da resposta. Esta versão anotada deve ficar visível para o aluno na fase de consulta.
- **Dashboard Estatístico**: um dashboard para os Administradores com métricas em tempo real (ex: média por disciplina, progresso global da correção, volume de pedidos de revisão...), utilizando bibliotecas de visualização de dados.
- **Sistema de Notificações**: notificações in-app e/ou por Email (usando ferramentas como [MailCrab](https://github.com/tweedegolf/mailcrab) para ambiente local) para informar os utilizadores de eventos críticos (ex: prova disponibilizada para consulta, prazo de revisão a terminar, nota de revisão atribuída).
- **Registo de Auditoria Inviolável**: Criar uma espécie de "log" que regista todas as ações críticas e quem as executou. Por exemplo: "Professor X alterou a nota do fragmento Y de 1.5 para 2.0 no dia Z". Se houver disputas num pedido de revisão, o administrador pode consultar este histórico.
- **Marcas de Água Dinâmicas**: Sempre que um aluno ou funcionário descarregar o PDF final da prova corrigida, o backend injeta uma marca de água (ex: o email de quem fez o download, data, IP...) em todas as páginas. Isto previne a partilha indevida e rastreia fugas de informação.

Para alcançar a **nota máxima de 20 valores**, deve implementar **quatro** das funcionalidades mencionadas acima, demonstrando uma excelente qualidade e modularidade de código, bem como uma UI/UX cuidada.

---

O sistema será utilizado por membros do Ministério da Deseduação e das escolas, e deve proporcionar uma interface intuitiva e funcional para a gestão dos exames nacionais.

É permitida e valorizada (mas **não obrigatória**) a implementação de funcionalidades adicionais às acima, especialmente se considerar que algum aspeto melhoraria significativamente a _User Experience (UX)_ durante a utilização do sistema (por exemplo, uma homepage, um dashboard, sistema de mensagens internas, etc.).

Deve realizar o exercício de forma modular. Serão valorizadas qualidade e estética do código e da interface web apresentada.

Dúvidas sobre os requisitos do exercício devem ser esclarecidas por email.

_Note-se que a situação descrita neste enunciado foi simplificada e não retrata necessariamente a realidade. A sua solução será usada apenas para fins de avaliação._

## Submissão

A entrega deve seguir as melhores práticas de desenvolvimento de software:

- É obrigatório e alvo de avaliação a utilização de um sistema de controlo de versões (especificamente, [git](https://git-scm.com/)) durante o desenvolvimento do exercício, com um histórico de commits lógico e descritivo.
- Deve incluir um ficheiro `README.md` com instruções claras de como configurar e executar o projeto localmente (a utilização de contentores Docker é valorizada). Inclua notas sobre as decisões de arquitetura e modelo de dados.
- Deve providenciar uma forma de preencher a base de dados com dados de teste (ex: `populate.sql` ou seeders da framework) que crie, no mínimo, **2 escolas, 10 alunos, 3 professores, disciplinas base e 1 Administrador**, permitindo a validação imediata da plataforma.

Não submeta artefactos irrelevantes (e.g., pasta `node_modules`).

O exercício prático tem a **nota mínima de 12 valores**. Soluções com nota inferior a 12 valores serão reprovadas.

**Prazo máximo de entrega: 22 de Agosto de 2026, às 23:59.**

Não serão aceites quaisquer entregas após o prazo limite, por qualquer razão. Qualquer candidato que não entregue uma solução até ao prazo limite será automaticamente avaliado com a cotação de 0 (zero) valores na componente de seleção correspondente ao exercício prático.

## Aviso Importante sobre o uso de Inteligência Artificial:

O uso de qualquer tipo de Ferramenta de IA é permitido e altamente aconselhado, desde que seja feita uma revisão ativa de todo o código gerado. Note-se que o código produzido por IA é, por vezes, desnecessariamente complexo, tendendo a criar design patterns inúteis e soluções over-engineered.

O candidato deverá conhecer e dominar todos os aspetos do seu código, sendo capaz de explicar como funciona e as razões por trás de cada decisão de implementação. Durante a entrevista, serão efetuadas perguntas detalhadas sobre a arquitetura e lógica do projeto, sendo estritamente esperado que o candidato saiba responder na totalidade, caso contrário, reservamos o direito de o excluir do concurso.

**Bom trabalho!**

## Recursos Potencialmente Úteis

- [https://spring.io/guides](https://spring.io/guides)
- [https://vuejs.org/guide/introduction.html](https://vuejs.org/guide/introduction.html)
- [https://vuetifyjs.com/en/](https://vuetifyjs.com/en/)