"""Executa o roteiro de aceitação (03-roteiro-para-testadores.md) contra a aplicação local
e imprime cada verificação com OK/FALHOU e o que foi observado."""
import http.cookiejar, json, re, sys, time, urllib.error, urllib.parse, urllib.request, zlib, struct, uuid

BASE = sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8090'
ADMIN_EMAIL, ADMIN_SENHA = sys.argv[2], sys.argv[3]
SUF = uuid.uuid4().hex[:6]
resultados = []


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


class Sessao:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar), NoRedirect)
        self.csrf = None

    def req(self, metodo, caminho, dados=None, arquivos=None, json_=False):
        url = BASE + caminho
        corpo, cab = None, {}
        if json_:
            cab['Accept'] = 'application/json'
        if metodo == 'POST':
            dados = dict(dados or {})
            dados['_csrf'] = self.csrf
            if arquivos:
                bnd = '----b' + uuid.uuid4().hex
                partes = []
                for k, v in dados.items():
                    partes.append(f'--{bnd}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
                for k, (nome, conteudo, tipo) in arquivos.items():
                    partes.append(f'--{bnd}\r\nContent-Disposition: form-data; name="{k}"; filename="{nome}"\r\nContent-Type: {tipo}\r\n\r\n'.encode() + conteudo + b'\r\n')
                partes.append(f'--{bnd}--\r\n'.encode())
                corpo = b''.join(partes)
                cab['Content-Type'] = 'multipart/form-data; boundary=' + bnd
            else:
                corpo = urllib.parse.urlencode(dados).encode()
                cab['Content-Type'] = 'application/x-www-form-urlencoded'
        r = urllib.request.Request(url, data=corpo, headers=cab, method=metodo)
        try:
            resp = self.op.open(r, timeout=20)
            status, texto, local = resp.status, resp.read(), resp.headers.get('Location')
        except urllib.error.HTTPError as e:
            status, texto, local = e.code, e.read(), e.headers.get('Location')
        texto = texto.decode('utf-8', 'replace') if isinstance(texto, bytes) else texto
        m = re.search(r'name="_csrf" value="([^"]+)"', texto) or re.search(r'name="_csrf" content="([^"]+)"', texto)
        if m:
            self.csrf = m.group(1)
        return status, texto, (local or '')

    def get(self, c, json_=False):
        return self.req('GET', c, json_=json_)

    def post(self, c, d=None, arquivos=None):
        return self.req('POST', c, d, arquivos)

    def login(self, email, senha):
        self.get('/login')
        return self.post('/login', {'email': email, 'senha': senha})


def check(grupo, tarefa, descricao, ok, obs=''):
    resultados.append({'grupo': grupo, 'tarefa': tarefa, 'desc': descricao, 'ok': bool(ok), 'obs': obs})
    print(f"[{'OK' if ok else 'FALHOU'}] G{grupo} T{tarefa} {descricao} :: {obs}")


def ids_por_nome(html):
    return {n.strip(): int(i) for i, n in re.findall(r'href="/cupcakes/(\d+)"[^>]*>([^<]+)</a>', html)}


# ======================== GRUPO 1: conta e acesso ========================
c = Sessao()
email = f'teste.{SUF}@teste.local'
c.get('/cadastro')
st, h, _ = c.post('/cadastro', {'nome': 'Teste Aceitacao', 'email': email, 'telefone': '(11) 91234-5678', 'senha': 'abc', 'confirmarSenha': 'abc'})
check(1, 1, 'Cadastro com senha fraca é recusado', 'no mínimo 8 caracteres' in h, 'mensagem "A senha deve ter no mínimo 8 caracteres, com letras e números."')
st, h, loc = c.post('/cadastro', {'nome': 'Teste Aceitacao', 'email': email, 'telefone': '(11) 91234-5678', 'senha': 'Cupcake2026', 'confirmarSenha': 'Cupcake2026'})
check(1, 1, 'Cadastro com dados válidos', st == 302 and loc.endswith('/login'), f'HTTP {st} → {loc}')
st, h, loc = c.get('/login')
check(1, 1, 'Mensagem de conta criada no login', 'Conta criada com sucesso' in h)
c.get('/cadastro')
st, h, _ = c.post('/cadastro', {'nome': 'Outro', 'email': email.upper(), 'telefone': '(11) 91234-5678', 'senha': 'Cupcake2026', 'confirmarSenha': 'Cupcake2026'})
check(1, 1, 'E-mail repetido (em maiúsculas) é recusado', 'já está cadastrado' in h)
st, h, loc = c.login(email, 'Cupcake2026')
check(1, 2, 'Login com a conta criada', st == 302 and loc.rstrip('/').endswith(':8090') or loc.endswith('/'), f'→ {loc}')
st, h, _ = c.get('/')
check(1, 2, 'Vitrine saúda o cliente pelo nome', 'Olá, Teste!' in h)
st, h, loc = c.post('/logout')
check(1, 16, 'Logout volta ao login', st == 302 and 'saiu' in loc, f'→ {loc}')
st, h, loc = c.get('/')
check(1, 16, 'Após sair, a vitrine exige login de novo', st == 302 and loc.endswith('/login'), f'→ {loc}')

b = Sessao()
email_b = f'bloqueio.{SUF}@teste.local'
b.get('/cadastro')
b.post('/cadastro', {'nome': 'Bloqueio Teste', 'email': email_b, 'telefone': '(11) 91234-5678', 'senha': 'Cupcake2026', 'confirmarSenha': 'Cupcake2026'})
locs = [b.login(email_b, 'senhaErrada1')[2] for _ in range(5)]
check(1, 2, '5 senhas erradas bloqueiam a conta', all('credenciais' in l for l in locs[:4]) and 'bloqueada' in locs[4], ', '.join(l.split('?')[-1] for l in locs))
st, h, loc = b.login(email_b, 'Cupcake2026')
check(1, 2, 'Conta bloqueada não entra nem com a senha certa', 'bloqueada' in loc)
b.get('/recuperar-senha')
st, h, _ = b.post('/recuperar-senha', {'email': email_b})
m = re.search(r'redefinir-senha\?token=([\w-]+)', h)
check(1, 15, 'Esqueci minha senha mostra o link (modo demonstração)', m is not None and 'Se houver uma conta' in h)
b.get('/redefinir-senha?token=' + m.group(1))
st, h, loc = b.post('/redefinir-senha', {'token': m.group(1), 'senha': 'NovaSenha2026', 'confirmarSenha': 'NovaSenha2026'})
check(1, 15, 'Senha redefinida pelo link', st == 302 and loc.endswith('/login'), f'→ {loc}')
st, h, loc = b.login(email_b, 'NovaSenha2026')
check(1, 15, 'Login com a nova senha e conta desbloqueada', 'erro' not in loc, f'→ {loc}')
b.get('/recuperar-senha')
st, h, _ = b.post('/redefinir-senha', {'token': m.group(1), 'senha': 'OutraSenha2026', 'confirmarSenha': 'OutraSenha2026'})
check(1, 15, 'O mesmo link não funciona duas vezes', 'inválido ou expirado' in h)

# ======================== GRUPO 2: vitrine e carrinho ========================
s = Sessao()
s.login(email, 'Cupcake2026')
st, h, _ = s.get('/')
cup = ids_por_nome(h)
cats = dict((n, int(i)) for i, n in re.findall(r'href="/\?categoria=(\d+)"[^>]*>([^<]+)</a>', h))
check(2, 3, 'Vitrine lista os cupcakes e as categorias', len(cup) >= 10 and len(cats) >= 5, f'{len(cup)} cupcakes, categorias: {", ".join(cats)}')
st, h, _ = s.get(f'/vitrine/grid?categoria={cats["Chocolate"]}')
nomes = set(ids_por_nome(h))
check(2, 3, 'Filtro "Chocolate"', nomes == {'Brigadeiro Gourmet', 'Chocolate Belga'}, ', '.join(sorted(nomes)))
st, h, _ = s.get('/vitrine/grid?busca=MORANGO')
nomes = set(ids_por_nome(h))
check(2, 4, 'Busca "MORANGO" (nome e ingrediente, sem diferenciar maiúsculas)', nomes == {'Morango Festa', 'Red Velvet Morango'}, ', '.join(sorted(nomes)))
st, h, _ = s.get('/vitrine/grid?busca=pizza')
check(2, 4, 'Busca sem resultado', 'Produto não encontrado' in h)
st, h, _ = s.get(f'/cupcakes/{cup["Nutella com Avelã"]}')
check(2, 5, 'Cupcake sem estoque não pode ser comprado', 'indisponível no momento' in h and 'data-adicionar' not in h)
st, h, _ = s.get(f'/cupcakes/{cup["Red Velvet Divine"]}')
check(2, 6, 'Detalhes com descrição, ingredientes, preço e botão de adicionar', all(x in h for x in ('Ingredientes', 'Impostos inclusos', 'Adicionar ao carrinho', 'R$ 14,90')))
st, h, _ = s.get(f'/api/carrinho?itens={cup["Red Velvet Divine"]}:25,{cup["Baunilha Sonho"]}:1', json_=True)
r = json.loads(h)
check(2, 7, 'Carrinho limita a 20 unidades por item e recalcula no servidor', r['itens'][0]['quantidade'] == 20 and abs(r['subtotal'] - (20 * 14.9 + 14.9)) < 0.01, f'subtotal R$ {r["subtotal"]:.2f}; aviso: {r["avisos"][0]}')
st, h, _ = s.get(f'/api/carrinho?itens={cup["Limão Siciliano"]}:1', json_=True)
r = json.loads(h)
check(2, 8, 'Carrinho abaixo de R$ 15,00 é sinalizado', r['atingiuValorMinimo'] is False, f'subtotal R$ {r["subtotal"]:.2f}')

# ======================== GRUPO 3: checkout e pagamento ========================
def frete(cep):
    st, h, _ = s.get('/api/frete?cep=' + cep, json_=True)
    return st, json.loads(h)

st, r = frete('20040020')
check(3, 9, 'CEP do Rio de Janeiro: fora da área', st == 422 and 'Ainda não entregamos' in r['erro'], r.get('erro', ''))
st, r = frete('00000000')
check(3, 9, 'CEP inexistente', st == 422 and 'CEP não encontrado' in r['erro'], r.get('erro', ''))
st, r = frete('01310100')
check(3, 9, 'CEP de São Paulo: frete R$ 7,00 / 45 min', st == 200 and r['valor'] == 7.0 and r['tempoEstimadoMin'] == 45, f'{r["endereco"]["logradouro"]}, {r["endereco"]["cidade"]}/{r["endereco"]["uf"]}')
st, r = frete('13010000')
check(3, 9, 'CEP de Campinas: frete R$ 15,00 / 120 min', st == 200 and r['valor'] == 15.0 and r['tempoEstimadoMin'] == 120, r['endereco']['cidade'])

def pedido(sessao, itens, forma, cartao=None):
    sessao.get('/checkout')
    d = {'itens': itens, 'cep': '01310-100', 'numero': '1000', 'complemento': '', 'logradouro': '', 'bairro': '', 'formaPagamento': forma}
    if cartao:
        d.update({'numeroCartao': cartao, 'nomeCartao': 'TESTE ACEITACAO', 'validadeCartao': '12/30', 'cvv': '123', 'parcelas': '2'})
    return sessao.post('/pedidos', d)

st, h, _ = pedido(s, f'{cup["Limão Siciliano"]}:1', 'DINHEIRO')
check(3, 8, 'Finalizar abaixo do mínimo mostra o aviso', 'pedido mínimo é de R$ 15,00' in h)
st, h, _ = pedido(s, f'{cup["Red Velvet Morango"]}:15', 'DINHEIRO')
check(3, 9, 'Quantidade acima do estoque é recusada', 'só temos' in h, re.search(r'Red Velvet Morango: só temos \d+ unidade\(s\)', h).group(0) if 'só temos' in h else '')
estoque_antes = int(re.search(r'data-max="(\d+)"', s.get(f'/cupcakes/{cup["Chocolate Belga"]}')[1]).group(1))
st, h, loc = pedido(s, f'{cup["Chocolate Belga"]}:2', 'CARTAO', '4000 0000 0000 0002')
check(3, 10, 'Cartão 4000 0000 0000 0002 é recusado', st == 200 and 'Pagamento não aprovado' in h)
estoque_depois = int(re.search(r'data-max="(\d+)"', s.get(f'/cupcakes/{cup["Chocolate Belga"]}')[1]).group(1))
check(3, 10, 'Recusa não altera o estoque', estoque_antes == estoque_depois, f'{estoque_antes} → {estoque_depois}')
st, h, loc = pedido(s, f'{cup["Chocolate Belga"]}:2', 'CARTAO', '4111 1111 1111 1111')
id_cartao = int(re.search(r'/pedidos/(\d+)', loc).group(1)) if '/pedidos/' in loc else None
st, h, _ = s.get(f'/pedidos/{id_cartao}?confirmado')
check(3, 11, 'Cartão 4111 aprovado: "Pedido confirmado!"', 'Pedido confirmado!' in h and 'Em preparação' in h and 'final 1111' in h, f'pedido #{id_cartao:05d}, 2x sem juros' if id_cartao else loc)
estoque_pos = int(re.search(r'data-max="(\d+)"', s.get(f'/cupcakes/{cup["Chocolate Belga"]}')[1]).group(1))
check(3, 11, 'Estoque baixa após a compra', estoque_pos == estoque_antes - 2, f'{estoque_antes} → {estoque_pos}')
st, h, loc = pedido(s, f'{cup["Pistache"]}:1', 'PIX')
id_pix = int(re.search(r'/pedidos/(\d+)', loc).group(1))
st, h, _ = s.get(f'/pedidos/{id_pix}?confirmado')
check(3, 12, 'PIX: pedido aguardando pagamento com QR Code e código', 'Aguardando pagamento' in h and 'Pague com PIX' in h and 'BR.GOV.BCB.PIX' in h)
st, png, _ = s.get(f'/pedidos/{id_pix}/pix.png')
check(3, 12, 'Imagem do QR Code é gerada', st == 200)
s.get(f'/pedidos/{id_pix}')
st, h, loc = s.post(f'/pedidos/{id_pix}/pix/simular-pagamento')
st, h, _ = s.get(f'/pedidos/{id_pix}')
check(3, 12, 'Simular pagamento do PIX leva a "Em preparação"', 'Pagamento PIX confirmado' in h and 'Em preparação' in h)

# ======================== GRUPO 4: acompanhamento, notificações, cancelamento ========================
st, h, _ = s.get('/pedidos')
check(4, 13, '"Meus pedidos" lista os pedidos com status e valor', f'#SB{id_cartao:05d}' in h and f'#SB{id_pix:05d}' in h)
st, h, _ = s.get('/')
cont = re.search(r'aria-label="Notificações">.*?class="badge"[^>]*>(\d+)<', h, re.S)
check(4, 13, 'Sino mostra notificações não lidas', cont is not None, f'{cont.group(1)} não lidas' if cont else '')
st, h, _ = s.get('/notificacoes')
check(4, 13, 'Notificações listam cada etapa do pedido', 'pagamento PIX recebido' in h and 'confirmado!' in h)
st, h, _ = s.get(f'/api/pedidos/{id_cartao}/status', json_=True)
check(4, 13, 'Status consultado para atualização em tempo real', json.loads(h)['status'] == 'EM_PREPARACAO')
estoque_antes = int(re.search(r'data-max="(\d+)"', s.get(f'/cupcakes/{cup["Pistache"]}')[1]).group(1))
s.get(f'/pedidos/{id_pix}')
s.post(f'/pedidos/{id_pix}/cancelar')
st, h, _ = s.get(f'/pedidos/{id_pix}')
estoque_depois = int(re.search(r'data-max="(\d+)"', s.get(f'/cupcakes/{cup["Pistache"]}')[1]).group(1))
check(4, 14, 'Cancelar pedido "Em preparação" devolve o estoque', 'Este pedido foi cancelado' in h and estoque_depois == estoque_antes + 1, f'Pistache {estoque_antes} → {estoque_depois}')
s.get(f'/pedidos/{id_pix}')
s.post(f'/pedidos/{id_pix}/cancelar')
st, h, _ = s.get(f'/pedidos/{id_pix}')
check(4, 14, 'Pedido cancelado não pode ser cancelado de novo', 'não pode mais ser cancelado' in h)

# ======================== GRUPO 5: administrador e segurança ========================
a = Sessao()
st, h, loc = a.login(ADMIN_EMAIL, ADMIN_SENHA)
check(5, 17, 'Admin entra direto no painel', loc.endswith('/admin'), f'→ {loc}')
st, h, _ = a.get('/admin')
check(5, 17, 'Painel com contadores e alerta de estoque baixo', 'Alerta de estoque baixo' in h and 'Nutella com Avelã' in h)

def png():
    def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    raw = b''.join(b'\x00' + b'\xff\x6b\x8a' * 8 for _ in range(8))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 8, 8, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b'')

form = {'nome': f'Cupcake Teste {SUF}', 'descricao': 'Cupcake criado no teste de aceitação', 'ingredientes': 'farinha, açúcar, baunilha', 'preco': '13.50', 'estoque': '8', 'categoriaId': str(cats['Especiais'])}
a.get('/admin/cupcakes/novo')
st, h, _ = a.post('/admin/cupcakes/novo', form, {'imagem': ('virus.png', b'isto nao e uma imagem', 'image/png')})
check(5, 18, 'Arquivo que não é imagem é recusado', 'Formato de imagem não suportado' in h)
st, h, loc = a.post('/admin/cupcakes/novo', form, {'imagem': ('foto.png', png(), 'image/png')})
st2, h2, _ = s.get('/vitrine/grid?busca=' + SUF)
check(5, 18, 'Novo cupcake com foto aparece na vitrine', st == 302 and f'Cupcake Teste {SUF}' in h2, f'→ {loc}')
novo_id = ids_por_nome(h2)[f'Cupcake Teste {SUF}']
a.get('/admin/estoque')
a.post(f'/admin/estoque/{novo_id}', {'quantidade': '0'})
st, h, _ = s.get('/vitrine/grid?busca=' + SUF)
check(5, 19, 'Estoque 0 deixa o cupcake "Indisponível" na vitrine', 'Indisponível' in h)
a.get(f'/admin/pedidos/{id_cartao}')
a.post(f'/admin/pedidos/{id_cartao}/status', {'status': 'ENTREGUE'})
st, h, _ = a.get(f'/admin/pedidos/{id_cartao}')
check(5, 20, 'Não é possível pular de "Em preparação" para "Entregue"', 'Não é possível mudar' in h)
a.post(f'/admin/pedidos/{id_cartao}/status', {'status': 'SAIU_PARA_ENTREGA'})
st, h, _ = s.get(f'/api/pedidos/{id_cartao}/status', json_=True)
check(5, 20, 'Cliente vê "Saiu para entrega" após a mudança do admin', json.loads(h)['status'] == 'SAIU_PARA_ENTREGA')
a.get(f'/admin/pedidos/{id_cartao}')
a.post(f'/admin/pedidos/{id_cartao}/status', {'status': 'ENTREGUE'})
st, h, _ = a.get(f'/admin/pedidos/{id_cartao}')
check(5, 20, 'Pedido avançado até "Entregue"', 'Pedido entregue: não pode mais ser alterado' in h)
a.post(f'/admin/pedidos/{id_cartao}/status', {'status': 'CANCELADO'})
st, h, _ = a.get(f'/admin/pedidos/{id_cartao}')
check(5, 20, 'Pedido entregue não pode ser cancelado', 'não podem ser cancelados' in h)
st, h, _ = s.get('/notificacoes')
check(5, 20, 'Cliente recebe as notificações de "saiu" e "entregue"', 'saiu para entrega' in h and 'entregue. Bom apetite' in h)
st, h, loc = pedido(s, f'{cup["Doce de Leite"]}:1', 'CARTAO', '4111 1111 1111 1111')
id_ativo = int(re.search(r'/pedidos/(\d+)', loc).group(1))
a.get('/admin/cupcakes')
a.post(f'/admin/cupcakes/{cup["Doce de Leite"]}/remover')
st, h, _ = a.get('/admin/cupcakes')
check(5, 18, 'Cupcake em pedido ativo não pode ser removido', 'está em pedidos em andamento' in h)
st, h, _ = s.get('/admin')
check(5, '-', 'Cliente não acessa o painel admin', st == 403, f'HTTP {st}')
o = Sessao()
email_o = f'outro.{SUF}@teste.local'
o.get('/cadastro')
o.post('/cadastro', {'nome': 'Outro Cliente', 'email': email_o, 'telefone': '(11) 91234-5678', 'senha': 'Cupcake2026', 'confirmarSenha': 'Cupcake2026'})
o.login(email_o, 'Cupcake2026')
st, h, _ = o.get(f'/pedidos/{id_ativo}')
check(5, '-', 'Cliente não vê pedido de outro cliente', st == 404, f'HTTP {st}')

json.dump(resultados, open(sys.argv[4], 'w'), ensure_ascii=False, indent=1)
print(f"\nTOTAL: {sum(r['ok'] for r in resultados)}/{len(resultados)} OK")
