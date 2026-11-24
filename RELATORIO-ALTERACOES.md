# Relatório técnico — remoção do menu flutuante e ajustes de interface

## 1. Identificação da versão

- Projeto: **Cap cut / Blindado**
- Branch: `main`
- Alterações de remoção do flutuante: commit `dbf43de`
- Ajustes de interface desta versão: commit posterior ao ajuste da interface
- Build oficial realizado pelo GitHub Actions após a validação do código.

## 1.1. Renomeação e package

- Nome visual do aplicativo: `Cap cut`.
- `namespace`: `com.Privvideo.app`.
- `applicationId`: `com.Privvideo.app`.
- Declarações de package, fontes Java/Kotlin, AIDL, manifesto e regras ProGuard do módulo `app` foram alinhadas ao novo package.
- `libauth` e `libadb` permaneceram nos packages próprios para preservar as dependências e a lógica existente.
- A migração não altera o fluxo de autorização Shizuku nem a transferência de replay.

## 2. Remoção do menu flutuante

A funcionalidade do menu flutuante foi removida do fluxo principal do aplicativo. Foram eliminados:

- O botão `btnFloating` do layout principal.
- O listener que chamava `openFloatingMenu()`.
- O método `openFloatingMenu()` da `MainActivity`.
- O import e a referência ao serviço `FloatingService`.
- A declaração do serviço flutuante no `AndroidManifest.xml`.
- As permissões específicas de overlay e foreground service usadas exclusivamente pelo painel:
  - `SYSTEM_ALERT_WINDOW`;
  - `FOREGROUND_SERVICE`;
  - `FOREGROUND_SERVICE_SPECIAL_USE`.
- Os arquivos smali do pacote `com.xyz.app`, incluindo os componentes do menu e seus controles.
- O `ModuleInstaller.java`, que não possuía mais chamadas após a retirada do painel.
- Os assets exclusivos do módulo removido.
- `AssetCipher.java` e `AssetSeal.java`, que ficaram sem consumidores depois da remoção do instalador de módulo.

A remoção foi feita sem alterar as telas de login, autorização da Key, conexão/validação do Shizuku, navegação principal, transferência de replay, diagnóstico e demais fluxos independentes do menu flutuante.

## 3. Menu lateral

Os botões de três traços foram fixados como primeiro elemento dos cabeçalhos de `Passador de replay`, transferência, `Texturas` e `Bypass SS`, ficando visualmente no lado esquerdo. A aba lateral continua abrindo pelo lado esquerdo, pois o `screenMode` usa `layout_gravity="start"` e animação horizontal a partir de `translationX` negativo.

Foi adicionada seleção visual exclusiva:

- `PASSADOR DE REPLAY` fica com `btn_primary` e fundo vermelho quando ativo.
- `TEXTURAS` fica com `btn_primary` e fundo vermelho quando ativo.
- `BYPASS SS` fica com `btn_primary` e fundo vermelho quando ativo.
- Os demais itens retornam para `btn_outline` imediatamente.

A função `setActiveMenuItem()` centraliza essa regra para impedir que mais de uma opção permaneça marcada.

## 4. Persistência da Key

A tela de login foi reorganizada verticalmente para seguir a sequência solicitada: campo de Key, botão `SALVAR KEY`, botão `LOGIN`, botão `GRUPO OFC` e status da validação. O botão `SALVAR KEY` grava a Key em `SharedPreferences`, e a Key salva é restaurada quando o app é aberto novamente.

## 5. Grupo oficial

Foi adicionado o botão `GRUPO OFC` abaixo da área de login. Ao tocar, o aplicativo abre:

<https://whatsapp.com/channel/0029VbCowiZGJP8MKaHW0k3k>

Caso não exista um aplicativo capaz de abrir o link, o usuário recebe uma mensagem de erro sem derrubar a Activity.

## 6. Menu lateral e seleção visual

Os botões de três traços foram posicionados no lado esquerdo dos cabeçalhos. A aba lateral permanece ancorada à esquerda. Uma camada transparente (`menuBackdrop`) cobre a área externa enquanto a aba está aberta; tocar nessa área fecha o menu com a mesma animação de saída.

O item ativo do menu recebe fundo vermelho e texto preto. Os itens inativos retornam ao fundo escuro de contorno e texto vermelho. Isso evita que o texto de `TEXTURAS` desapareça ao trocar para outra seção.

## 7. Validações técnicas

Foram verificadas:

- Ausência de referências a `btnFloating`.
- Ausência de referências a `FloatingService` e `openFloatingMenu`.
- Ausência da declaração do serviço de overlay no manifesto.
- Ausência das permissões específicas de overlay.
- XML do manifesto e layout com estrutura válida.
- Referências do novo `CheckBox` e do botão `GRUPO OFC` alinhadas entre XML e Kotlin.
- `git diff --check` sem erros de whitespace.

A compilação final deve ser considerada válida somente após a conclusão bem-sucedida do workflow do GitHub Actions associado ao commit final. Não foi afirmado teste em dispositivo físico.
