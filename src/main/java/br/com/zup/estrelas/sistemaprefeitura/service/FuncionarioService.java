package br.com.zup.estrelas.sistemaprefeitura.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.zup.estrelas.sistemaprefeitura.entity.FuncionarioEntity;
import br.com.zup.estrelas.sistemaprefeitura.entity.SecretariaEntity;
import br.com.zup.estrelas.sistemaprefeitura.repository.FuncionarioRepository;
import br.com.zup.estrelas.sistemaprefeitura.repository.SecretariaRepository;
import br.com.zup.estrelas.sistemprefeitura.dto.AlteraFuncionarioDTO;
import br.com.zup.estrelas.sistemprefeitura.dto.MensagemDTO;

@Service
public class FuncionarioService implements IFuncionarioService {

	private static final String FUNCIONARIO_ALTERADO_COM_SUCESSO = "Funcionário alterado com sucesso.";
	private static final String FUNCIONARIO_REMOVIDO_COM_SUCESSO = "Funcionário removido com sucesso!";
	private static final String FUNCIONARIO_CADASTRADO_COM_SUCESSO = "Funcionário cadastrado com sucesso.";
	private static final String FUNCIONARIO_INEXISTENTE = "Funcionário inexistente.";
	private static final String FUNCIONARIO_SEM_SECRETARIA = "Funcionário sem secretaria";
	private static final String SECRETARIA_INEXISTENTE = "Secretaria inexistente";
	private static final String NAO_HÁ_ORCAMENTO_PARA_ESSE_FUNCIONARIO = "Não há orçamento para esse funcionário";
	private static final String SALARIO_INFERIOR_ZERO = "Salário inferior a zero";
	private static final String ALTERACAO_REALIZADA_COM_SUCESSO = "Alteração de secretaria realizada com sucesso";
	private static final String SALARIO_NAO_PODE_SER_REDUZIDO = "Salário não pode ser reduzido";
	private static final String ID_JÁ_UTILIZADO = "Já existe um funcionãrio com esse ID";

	@Autowired
	FuncionarioRepository funcionarioRepository;

	@Autowired
	SecretariaRepository secretariaRepository;

	// FIXME: Era importante aqui usar um DTO.
	public MensagemDTO adicionaFuncionario(FuncionarioEntity funcionario) {

	    // FIXME: Este método também está grande, dava pra criar métodos privados
	    // para fazer as validações também.
		if (funcionario.getIdFuncionario() != null) {

			if (funcionarioRepository.existsById(funcionario.getIdSecretaria())) {
				return new MensagemDTO(ID_JÁ_UTILIZADO);
			}
		}

		if (funcionario.getSecretaria() == null && funcionario.getIdSecretaria() == null) {
			return new MensagemDTO(FUNCIONARIO_SEM_SECRETARIA);
		}

		Optional<SecretariaEntity> secretariaOptional = secretariaRepository.findById(funcionario.getIdSecretaria());
		SecretariaEntity secretaria = secretariaOptional.get();

		if (secretaria == null) {
			return new MensagemDTO(SECRETARIA_INEXISTENTE);
		}

		funcionario.setSecretaria(secretaria);

		if (funcionario.getSalario() < 0) {
			return new MensagemDTO(SALARIO_INFERIOR_ZERO);
		}
		if (funcionario.getSalario() > secretaria.getOrcamentoFolha()) {
			return new MensagemDTO(NAO_HÁ_ORCAMENTO_PARA_ESSE_FUNCIONARIO);

		}

		secretaria.setOrcamentoFolha(secretaria.getOrcamentoFolha() - funcionario.getSalario());

		funcionarioRepository.save(funcionario);
		return new MensagemDTO(FUNCIONARIO_CADASTRADO_COM_SUCESSO);

	}

	public MensagemDTO alteraFuncionarioSecretaria(FuncionarioEntity funcionario) {

		if (funcionario.getIdFuncionario() == null) {
			return new MensagemDTO(FUNCIONARIO_INEXISTENTE);
		}
		if (funcionario.getSecretaria() == null && funcionario.getSecretaria().getIdSecretaria() == null) {
			return new MensagemDTO(FUNCIONARIO_SEM_SECRETARIA);
		}

		Optional<FuncionarioEntity> funcionarioAntigoOptional = funcionarioRepository
				.findById(funcionario.getSecretaria().getIdSecretaria());
		FuncionarioEntity funcionarioAntigo = funcionarioAntigoOptional.get();

		Optional<SecretariaEntity> secretariaNovaOptional = secretariaRepository
				.findById(funcionario.getSecretaria().getIdSecretaria());

		// FIXME: Utilizar o isPresent sempre antes de fazer um get.
		SecretariaEntity secretariaNova = secretariaNovaOptional.get();

		// FIXME: Essa validação não funciona pois se vc ter um get no optional empty 
		// estoura a exceção NoSuchElementException e não chega aqui nesse trecho de código
		// (vale pra todos que eu comentei sobre o isPresent).
		if (secretariaNova == null) {
			return new MensagemDTO(SECRETARIA_INEXISTENTE);
		}

		Optional<SecretariaEntity> secretariaAntigaOptional = secretariaRepository
				.findById(funcionario.getSecretaria().getIdSecretaria());
		// FIXME: Utilizar o isPresent sempre antes de fazer um get.
		SecretariaEntity secretariaAntiga = secretariaAntigaOptional.get();

		if (funcionario.getSalario() < 0) {
			return new MensagemDTO(SALARIO_INFERIOR_ZERO);
		}

		if (funcionario.getSalario() > secretariaNova.getOrcamentoFolha()) {
			return new MensagemDTO(NAO_HÁ_ORCAMENTO_PARA_ESSE_FUNCIONARIO);

		}
		secretariaNova.setOrcamentoFolha(secretariaNova.getOrcamentoFolha() - funcionario.getSalario());

		secretariaAntiga.setOrcamentoFolha(secretariaAntiga.getOrcamentoFolha() + funcionario.getSalario());

		if (funcionarioAntigo.getSalario() < funcionario.getSalario()) {
			return new MensagemDTO(SALARIO_NAO_PODE_SER_REDUZIDO);
		}

		secretariaRepository.save(secretariaAntiga);
		secretariaRepository.save(secretariaNova);

		funcionarioRepository.save(funcionario);
		return new MensagemDTO(ALTERACAO_REALIZADA_COM_SUCESSO);

	}

	public FuncionarioEntity buscaFuncionario(Long idFuncionario) {

		return funcionarioRepository.findById(idFuncionario).orElse(null);

	}

	public List<FuncionarioEntity> listaFuncionarios() {

		return (List<FuncionarioEntity>) funcionarioRepository.findAll();
	}

	public MensagemDTO removeFuncionario(Long idFuncionario) {

		if (idFuncionario != null && funcionarioRepository.existsById(idFuncionario)) {

			Optional<SecretariaEntity> secretariaOptional = secretariaRepository.findById(idFuncionario);
			// FIXME: Utilizar o isPresent sempre antes de fazer um get.
			SecretariaEntity secretaria = secretariaOptional.get();

			Optional<FuncionarioEntity> funcionarioOptional = funcionarioRepository.findById(idFuncionario);
			// FIXME: Utilizar o isPresent sempre antes de fazer um get.
			FuncionarioEntity funcionario = funcionarioOptional.get();

			secretaria.setOrcamentoFolha(secretaria.getOrcamentoFolha() + funcionario.getSalario());

			funcionarioRepository.deleteById(idFuncionario);

			return new MensagemDTO(FUNCIONARIO_REMOVIDO_COM_SUCESSO);
		}

		return new MensagemDTO(FUNCIONARIO_INEXISTENTE);
	}

	public MensagemDTO alteraFuncionario(Long idFuncionario, AlteraFuncionarioDTO alteraFuncionarioDTO) {

		Optional<FuncionarioEntity> funcionarioConsultado = funcionarioRepository.findById(idFuncionario);

		if (funcionarioConsultado.isPresent()) {

			FuncionarioEntity funcionarioAlterado = funcionarioConsultado.get();

			funcionarioAlterado.setNome(alteraFuncionarioDTO.getNome());
			funcionarioAlterado.setCpf(alteraFuncionarioDTO.getCpf());
			funcionarioAlterado.setSalario(alteraFuncionarioDTO.getSalario());
			funcionarioAlterado.setFuncao(alteraFuncionarioDTO.getFuncao());
			funcionarioAlterado.setConcursado(alteraFuncionarioDTO.getConcursado());
			funcionarioAlterado.setFuncao(alteraFuncionarioDTO.getFuncao());
			funcionarioAlterado.setDataAdmissao(alteraFuncionarioDTO.getDataAdmissao());

			funcionarioRepository.save(funcionarioAlterado);

			return new MensagemDTO(FUNCIONARIO_ALTERADO_COM_SUCESSO);

		}
		return new MensagemDTO(FUNCIONARIO_INEXISTENTE);
	}

}
