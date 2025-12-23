import PropTypes from 'prop-types';
import { Card, Col, Row } from 'react-bootstrap';

const StatCard = ({ title, value, icon, color }) => (
  <Card className="shadow-sm border-0 h-100 card-shadow">
    <Card.Body>
      <Row className="align-items-center g-0">
        <Col xs={8}>
          <h6 className="text-muted mb-1">{title}</h6>
          <h3 className="fw-bold mb-0">{value}</h3>
        </Col>
        <Col xs={4} className="text-end">
          <span className={`display-6 text-${color}`}>{icon}</span>
        </Col>
      </Row>
    </Card.Body>
  </Card>
);

StatCard.propTypes = {
  title: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  icon: PropTypes.node.isRequired,
  color: PropTypes.string
};

StatCard.defaultProps = {
  color: 'primary'
};

export default StatCard;
